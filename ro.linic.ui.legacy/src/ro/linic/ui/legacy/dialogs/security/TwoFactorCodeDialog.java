package ro.linic.ui.legacy.dialogs.security;

import static ro.colibri.util.StringUtils.isEmpty;
import static ro.flexbiz.util.commons.PresentationUtils.NEWLINE;
import static ro.linic.ui.legacy.session.UIUtils.setFont;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.opal.commons.SWTGraphicUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import ro.flexbiz.util.commons.LocalDateUtils;
import ro.flexbiz.util.commons.StringUtils;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpHeaders;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.http.RestCaller.PostConfigurer;
import ro.linic.ui.legacy.LegacyAuthenticationManager;
import ro.linic.ui.legacy.dialogs.Messages;
import ro.linic.ui.legacy.preferences.PreferenceKey;
import ro.linic.ui.legacy.session.ClientSession;

public class TwoFactorCodeDialog extends TitleAreaDialog {
	private static final ILog log = ILog.of(TwoFactorCodeDialog.class);

	// Widgets
	private Text code;

	private IEclipseContext ctx;
	private GenericValue loginResponse;

	private HttpResponse<String> twoFactorResponse;

	public TwoFactorCodeDialog(final Shell parent, final IEclipseContext ctx, final GenericValue loginResponse) {
		super(parent);
		this.ctx = ctx;
		this.loginResponse = loginResponse;
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Control contents = super.createContents(parent);
		setTitle(Messages.TwoFactorTitle);
		setMessage(Messages.TwoFactorDescription);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return contents;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final StringBuilder sb = new StringBuilder();
		sb.append(loginResponse.getList("factorTypeEnumIds").stream()
				.map(Object::toString)
				.map(Messages::getString)
				.map(factorType -> "\t\u2022 "+factorType)
				.collect(Collectors.joining(NEWLINE, Messages.TwoFactorDescriptionLong+NEWLINE, NEWLINE)));
		
		loginResponse.getList("sendableFactors").stream().forEach(f -> {
			final GenericValue factor = GenericValue.of("", "", (Map<? extends String, ? extends Object>) f);
			sb.append(NEWLINE).append(MessageFormat.format("<b>"+Messages.TwoFactor_CodeSent+"</b>", factor.getString("factorOption")));
			sendCode(factor);
		});
		
		final StyledText factors = new StyledText(container, SWT.WRAP | SWT.READ_ONLY);
		factors.setText(sb.toString());
		SWTGraphicUtil.applyHTMLFormating(factors);
		setFont(factors);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(factors);
		
		final Label codeLabel = new Label(container, SWT.NULL);
		codeLabel.setText(Messages.TwoFactor_Code);
		setFont(codeLabel);

		code = new Text(container, SWT.SINGLE | SWT.BORDER);
		setFont(code);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(code);
		code.setFocus();

		code.addModifyListener(e -> validate());
		return area;
	}

	private void sendCode(final GenericValue factor) {
		final String credentials = ClientSession.instance().getUsername() + ":" + ClientSession.instance().getPassword();
		final byte[] encodedAuth = Base64.encodeBase64(credentials.getBytes(StandardCharsets.ISO_8859_1));
		final String authHeader = "Basic " + new String(encodedAuth);
		
		RestCaller.post(UIUtils.moquiBaseUrl()+"/Login/sendOtp")
		.addHeader(HttpHeaders.AUTHORIZATION, authHeader)
		.addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
		.addHeader(HttpHeaders.ACCEPT, "application/json")
		.body(BodyProvider.of(GenericValue.of("", "", "factorId", factor.getString("factorId"))))
		.async(t -> UIUtils.showException(t, ctx.get(UISynchronize.class)));
	}

	private void validate() {
		if (isEmpty(code.getText())) {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			setErrorMessage(Messages.TwoFactor_MissingCode);
			return;
		}

		getButton(IDialogConstants.OK_ID).setEnabled(true);
		setErrorMessage(null);
	}

	@Override
	protected void okPressed() {
		if (isEmpty(code.getText())) {
			setErrorMessage(Messages.TwoFactor_MissingCode);
			return;
		}
		
		final Bundle bundle = FrameworkUtil.getBundle(PreferenceKey.class);
		final ISecurePreferences root = SecurePreferencesFactory.getDefault();
 		final ISecurePreferences secureNode = root.node(bundle.getSymbolicName());
 		
 		String deviceId = null, deviceSecret = null;
 		try {
 			deviceId = secureNode.get(LegacyAuthenticationManager.DEVICE_ID_KEY, null);
 			deviceSecret = secureNode.get(LegacyAuthenticationManager.DEVICE_SECRET_KEY, null);
		} catch (final StorageException e) {
			log.error("Error getting secure preferences", e);
		}

		final PostConfigurer rest = RestCaller.post(UIUtils.moquiBaseUrl()+"/rest/login");
		if (!StringUtils.isEmpty(deviceId))
			rest.addHeader("Cookie", "deviceId="+deviceId+"; deviceSecret="+deviceSecret);
		
		final Version v = bundle.getVersion();
		final Optional<HttpResponse<String>> loginResponse = rest
				.addHeader(HttpHeaders.USER_AGENT, String.format("%s %d.%d.%d %s", "Flexbiz Desktop", v.getMajor(), v.getMinor(), v.getMicro(), UIUtils.getHostName()))
				.addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.addHeader(HttpHeaders.ACCEPT, "application/json")
				.body(BodyProvider.of(GenericValue.of("", "", "username", ClientSession.instance().getUsername(),
						"password",  ClientSession.instance().getPassword(),
						"code", code.getText(),
						"trustDevice", true, "trustThruDate", LocalDateUtils.POSTGRES_MAX)))
				.syncRaw(BodyHandlers.ofString(), t -> UIUtils.showException(t, ctx.get(UISynchronize.class)))
				.map(res -> {
					final GenericValue response = HttpUtils.fromJSON(res.body(), GenericValue.class);
					if (response.getBoolean("loggedIn"))
						return res;
					log.error("2FA login RESPONSE: "+res.body());
					return null;
				});
		
		if (loginResponse.isPresent()) {
			twoFactorResponse = loginResponse.get();
			try {
				final Optional<String> deviceIdResp = HttpUtils.extractCookie(loginResponse.get().headers(), LegacyAuthenticationManager.DEVICE_ID_KEY);
				final Optional<String> deviceSecretResp = HttpUtils.extractCookie(loginResponse.get().headers(), LegacyAuthenticationManager.DEVICE_SECRET_KEY);
				if (deviceIdResp.isPresent())
					secureNode.put(LegacyAuthenticationManager.DEVICE_ID_KEY, deviceIdResp.get(), true);
				if (deviceSecretResp.isPresent())
					secureNode.put(LegacyAuthenticationManager.DEVICE_SECRET_KEY, deviceSecretResp.get(), true);
				secureNode.flush();
			} catch (final IOException | StorageException e) {
				log.error(e.getMessage(), e);
			}
			super.okPressed();
		}
		else {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			setErrorMessage(Messages.TwoFactor_InvalidCode);
		}
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	public HttpResponse<String> response() {
		return twoFactorResponse;
	}
}