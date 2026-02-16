 
package ro.linic.ui.legacy.expressions;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Evaluate;

import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.security.services.AuthenticationSession;

public class BetaTester {
	private static final ILog log = ILog.of(BetaTester.class);
	public static final String BETA_TESTER_PREF_KEY = "BETA_TESTER"; //$NON-NLS-1$
	
	@Evaluate
	public boolean evaluate(final AuthenticationSession authSession) {
		return RestCaller.get("/rest/s1/mantle/my/preference")
				.internal(authSession.authentication())
				.addUrlParam("preferenceKey", BETA_TESTER_PREF_KEY)
				.sync(GenericValue.class, t -> log.error(t.getMessage(), t))
				.map(gv -> gv.getBoolean("preferenceValue"))
				.orElse(false);
	}
}
