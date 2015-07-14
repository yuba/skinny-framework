package skinny.controller.feature

import skinny.engine.csrf.CsrfTokenGenerator

/**
 * Angular.js Cross Site Request Forgery (XSRF) Protection support.
 *
 * https://docs.angularjs.org/api/ng/service/$http#cross-site-request-forgery-xsrf-protection
 */
trait AngularXSRFCookieProviderFeature { self: BeforeAfterActionFeature =>

  protected def xsrfCookieName: String = AngularJSSpecification.xsrfCookieName

  beforeAction() {
    if (cookies.get(xsrfCookieName).isEmpty) {
      cookies += (xsrfCookieName -> CsrfTokenGenerator())
    }
  }

}
