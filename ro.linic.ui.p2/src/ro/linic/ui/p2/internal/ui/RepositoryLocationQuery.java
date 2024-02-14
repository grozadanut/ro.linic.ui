package ro.linic.ui.p2.internal.ui;

import java.net.URI;

import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;

/**
 * RepositoryLocationQuery yields true for all URI elements.
 *
 * @since 3.5
 */
public class RepositoryLocationQuery extends ExpressionMatchQuery<URI> {

	public RepositoryLocationQuery() {
		super(URI.class, ExpressionUtil.TRUE_EXPRESSION);
	}
}
