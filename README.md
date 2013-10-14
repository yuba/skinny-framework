# Skinny Framework 

Skinny is a full-stack web app framework, which is built on [Scalatra](http://scalatra.org) and additional components are integrated. 

To put it simply, Skinny framework's concept is **Scala on Rails**. Skinny is highly inspired by [Ruby on Rails](http://rubyonrails.org/) and it is optimized for sustainable productivity for ordinary Servlet-based app development. 

**[Notice]** Still in alpha stage. Architecture and API compatibility won't be kept until 1.0 release.

### Why Skinny?

What does the name of `Skinny` actually mean?

#### Application should be skinny

All the parts of web application - controllers, models, views, routings and other settings - should be skinny. If you use Skinny framework, you don't need to have non-essential code anymore. For instance, when you create a simple registration form, all you need to do is just defining parameters and validation rules and creating view tempaltes in an efficient way (ssp, scaml, jade, FreeMarker or something else) in most cases.

#### Framework should be skinny

Even if you need to investigate Skinny's inside, don't worry. Skinny keeps itself skinny, too. I believe that if the framework is well-designed, eventually the implemetation is skinny. 

#### "su-ki-ni" in Japanese means "as you like it"

A sound-alike word **"好きに (su-ki-ni)"** in Japanese means **"as you like it"**. This is only half kidding but it also represents Skinny's concept. Skinny framework should provide flexible APIs to empower developers as much as possible and shouldn't bother them.

## How to use

Actually, An application built with Skinny framework is a Scalatra application. After preparing Scalatra app, just add the following dependency to your `project/Build.scala`.

```scala
libraryDependencies ++= Seq(
  "com.gitub.seratch" %% "skinny-framework" % "[0.9,)",
  "com.gitub.seratch" %% "skinny-test"      % "[0.9,)" % "test"
)
```

If you need only Skinny-ORM or Skinny-Validator, you can use only what you need. Even if you're a Play2 (or any others) user, these components are available for you as well.

```scala
libraryDependencies ++= Seq(
  "com.gitub.seratch" %% "skinny-orm"       % "[0.9,)",
  "com.gitub.seratch" %% "skinny-validator" % "[0.9,)",
  "com.gitub.seratch" %% "skinny-test"      % "[0.9,)" % "test"
)
```

## Try example

You can try the example right now.

https://github.com/seratch/skinny-framework/tree/develop/example

```
git clone https://github.com/seratch/skinny-framework.git
cd skinny-framework
sbt 
// project example
// ~;container:stop;container:start
```

Access `http://localhost:8080/example/` from your browser.

### Yeoman generator

or If you're familiar with [Yeoman](http://yeoman.io), a generator for [Skinny framework](https://github.com/seratch/skinny-framework) is available.

```sh
npm install -g yo
npm install -g generator-skinny
mkdir skinny-app
cd skinny-app
yo skinny
cd skinny
./skinny run
```

## Components

### Routing & Controller & Validator

Skinny's routing mechanism and controller layer on MVC architecture is a **rich Scalatra**. Skinny's extension provides you much simpler syntax. Of course, if you need to use Scalatra's API directly, Skinny never bother you.

`SkinnyController` is a class which extends `ScalatraBase` and out-of-the-box components are integrated. 

```scala
// src/main/scala/controller/MembersController.scala

class MembersController extends SkinnyController {
  protectFromForgery()

  beforeAction(only = Seq('index, 'new)) { set("countries", Country.findAll()) }

  def index = {
    // set 'members' in the request scope, then you can use it in views
    set("members" -> Member.findAll())
    render("/members/index")
  }

  def newOne = render("/members/new")

  def createForm = validation(
    paramKey("name") is required & minLength(2), 
    paramKey("countryId") is numeric
  )

  def createFormParams = params.permit(
    "groupId" -> ParamType.Int , "countryId" -> ParamType.Long)

  def create = if (createForm.validate()) {
    Member.createWithAttributes(createFormParams)
    redirect("/members")
  } else {
    render("/members/new")
  }
}

// src/main/scala/ScalatraBootstrap.scala

class ScalatraBootstrap exnteds SkinnyLifeCycle {
  override def initSkinnyApp(ctx: ServletContext) {
    // register routes
    ctx.mount(new MembersController with Routes {
      get("/members/?")(index).as('index)
      get("/members/new")(newOne).as('new)
      post("/members/?")(create).as('create)
    }, "/*")
  }
}
```

Skinny-Validator is newly created validator which is based on [seratch/inputvalidator](https://github.com/seratch/inputvalidator) and much improved. Rules are so simple that you can easily add original validation rules. Furthermore, you can use this validator with any other frameworks.

```scala
import skinny.validator._

def createForm = validation(
  paramKey("name") is required & minLength(2) & alphabetOnly, 
  paramKey("countryId") is numeric
)

object alphabetOnly extends ValidationRule {
  def name = "alphabetOnly"
  def isValid(v: Any) = v == null || v.toString.matches("^[a-zA-Z]*$")
}
```

`SkinnyResource` which is similar to Rails ActiveResource is available. That's a pretty DRY way.

```scala
object CompaniesController extends SkinnyResource {
  protectFromForgery()

  override def skinnyCRUDMapper = Company
  override def resourcesName = "companies"
  override def resourceName = "company"

  override def createForm = validation(paramKey("name") is required & maxLegnth(64), paramKey("registrationCode" is numeric)
  override def createFormStrongParameters = Seq("name" -> ParamType.String, "registrationCode" -> ParamType.Int)

  override def updateForm = validation(paramKey("name") is required & maxLegnth(64))
  override def updateFormStrongParameters = Seq("name" -> ParamType.String)
}
```

Company object should extend `SkinnyCRUDMapper` and you should prepare some view templates under `src/main/webapp/WEB-INF/views/members/`.

### ORM

Skinny provides you Skinny-ORM as the default O/R mapper, which is built with [ScalikeJDBC](https://github.com/seratch/scalikejdbc).

Skinny-ORM is simple but much powerful. Your first model class and companion are here.

```scala
case class Member(id: Long, name: String, createdAt: DateTime)

object Member extends SkinnyCRUDMapper[Member] {

  // only define ResultSet extractor at minimum
  override def extract(rs: WrappedResultSet, n: ResultName[Member]): Member = new Member(
    id = rs.long(n.id),
    name = rs.string(n.name),
    createdAt = rs.dateTime(n.createdAt)
  )
}
```

That's all! Now you can use the following APIs.

```scala
Member.withAlias { m => // or "val m = Member.defaultAlias"

  // find by primary key
  val member: Option[Member] = Member.findById(123)
  val members: List[Member] = Member.findByIds(123, 234, 345)

  // find many
  val members: List[Member] = Member.findAll()
  val groupMembers = Member.findAllBy(sqls.eq(m.groupName, "Scala Users Group"))

  // count
  val allCount: Long = Member.countAll()
  val count = Member.countBy(sqls.isNotNull(m.deletedAt).and.eq(m.countryId, 123))

  // create with stong parameters
  val params = Map("name" -> "Bob")
  val id = Member.createWithAttributes(params.permit("name" -> ParamType.String))

  // create with named values
  val column = Member.column
  Member.createWithNamedValues(
    column.id -> 123,
    column.name -> "Chris",
    column.createdAt -> DateTime.now  
  )

  // update with strong parameters
  Member.updateById(123).withAttributes(params.permit("name" -> ParamType.String))

  // delete
  Member.deleteById(234)
}
```

If you need to join other tables, just add `belongsTo`, `hasOne` or `hasMany` (`hasManyThrough`) to the companion.

**[Notice]** Unfortunately, Skinny-ORM doesn't retrieve nested associations (e.g. members.head.groups.head.country) automatically though we're still seeking a way to resolove this issue.

```scala
class Member(id: Long, name: String, companyId: Long, company: Option[Company] = None, skills: Seq[Skill] = Nil)
object Member extends SkinnyCRUDMapper[Member] {

  // If byDefault is called, this join condition is enabled by default
  belongsTo[Company](Company, (m, c) => m.copy(company = Some(c))).byDefault

  val skills = hasManyThrough[Skill](
    MemberSkill, Skill, (m, skills) => m.copy(skills = skills))
}

Member.findById(123) // without skills
Member.joins(Member.skills).findById(123) // with skills
```

If you need to add methods, just write methods that use ScalikeJDBC' APIs directly.

```scala
object Member extends SkinnyCRUDMapper[Member] {
  val m = defaultAlias
  def findByGroupId(groupId: Long)(implicit s: DBSession = autoSession): List[Member] = {
    withSQL { select.from(Member as m).where.eq(m.groupId, groupId) }
      .map(apply(m)).list.apply()
  }
}
```

`timetamps` from `ActiveRecord` is available as the `TimestampsFeature` trait.

```scala
class Member(id: Long, name: String, createdAt: DateTime, updatedAt: Option[DateTime] = None)
object Member extends SkinnyCRUDMapper[Member] with TimestampsFeature[Member]
/* -- expects following table by default
create table meber(
  id bigint auto_increment primary key not null,
  name varchar(128) not null,
  created_at timestamp not null,
  updated_at timestamp
);
*/
```

Soft delete support is also available.

```scala
object Member extends SkinnyCRUDMapper[Member] with SoftDeleteWithTimestamp[Member]
/* -- expects following table by default
create table meber(
  id bigint auto_increment primary key not null,
  name varchar(128) not null,
  deleted_at timestamp
);
*/
```

Furthermore, optimistic lock is also available.

```scala
object Member extends SkinnyCRUDMapper[Member] with OptimisticLockWithVersionFeature[Member]
/* -- expects following table by default
create table meber(
  id bigint auto_increment primary key not null,
  name varchar(128) not null,
  lock_version bigint
);
*/
```


### View Templates

Skinny framework basically follows Scalatra's ScalateSupport, but Skinny has an additional convention.

Templates' path should be `{path}.{format}.{extension}`. Expeted {format} are `html`, `json`, `js` and `xml`.

The following ssp is `src/main/webapp/WEB-INF/views/members/index.html.ssp`.

```scala
<%@val members: Seq[model.Member] %>
<h3>Members</h3>
<hr/>

<table class="table table-bordered">
<thead>
  <tr>
    <th>ID</th>
    <th>Name</th>
    <th></th>
  </tr>
</thead>
<tbody>
  #for (member <- members)
  <tr>
    <td>${member.id}</td>
    <td>${member.name}</td>
    <td>
      <a href="/members/${member.id}/edit" class="btn btn-info">Edit</a>
      <a data-method="delete" data-confirm="Are you sure?" href="/members/${member.id}" class="btn btn-danger">Delete</a>
    </td>
  </tr>
  #end
</tbody>
</table>
```

Your controller code will be like this:

```scala
class MembersController extends SkinnyController {
  def index = {
    set("members", Member.findAll())
    render("/members/index")
  }
}
```

If you need to customize view templates, override the settings.

```scala
class MembersController extends SkinnyServlet {
  override val scalateExtension = "scaml"
}
```

```scala
-@val members: Seq[model.Member]
%h3 Members
%hr

%table(class="table table-bordered")
%thead
 %tr
  %th ID
  %th Name
  %th
%tbody
 -for(member <- members)
  %tr
   %td #{member.id}
   %td #{member.name}
   %td
    %a(href={"/members/"+member.id+"/edit"} class="btn btn-info") Edit
    %a(data-method="delete" data-confirm="Are you sure?" href={"/members/"+member.id} class="btn btn-danger") Delete
```

### Testing support

You can use Scalatra's great test support. Some optional feature is provieded by skinny-test library.

```scala
class ControllerSpec extends ScalatraFlatSpec with SkinnyTestSupport {
  addFilter(MembersController, "/*")

  it should "show index page" in {
    withSession("userId" -> "Alice") {
      get("/members") { status should equal(200) }
    }
  }
}
```

You can see some examples here:

https://github.com/seratch/skinny-framework/tree/develop/example/src/test/scala

### FactoryGirl

Though Skinny's FactoryGirl is not a complete port of [thoughtbot/factory_girl](https://github.com/thoughtbot/factory_girl), this module will be quite useful when testing your apps.

```scala
case class Company(id: Long, name: String)
object Company extends SkinnyCRUDMapper[Company] {
  def extract ...
}

val company1 = FactoryGirl(Company).create()
val company2 = FactoryGirl(Company).create("name" -> "FactoryPal, Inc.")

val country = FactoryGirl(Country, "countryyy").create()

val memberFactory = FactoryGirl(Member).withValues("countryId" -> country.id)
val member = memberFactory.create("companyId" -> company1.id, "createdAt" -> DateTime.now)
```

Settings is not in yaml files but typesafe-config conf file. In this example, `src/test/resources/factories.conf` is like this:

```
countryyy {
  name="Japan"
}
member {
  countryId="#{countryId}"
}
company {
  name="FactoryGirl, Inc."
}
name {
  first="Kazuhiro"
  last="Sera"
}
skill {
  name="Scala Programming"
}
```

### TODO

These are major tasks that Skinny should fix.

 - Scaffold generator support
 - Designing Authentication API
 - CoffeeScript and so on (basically wro4j)
 - Documentation (wiki)

Your feedback or pull requests are always welcome.

## License

(The MIT License)

Copyright (c) 2013 Kazuhiro Sera @seratch


