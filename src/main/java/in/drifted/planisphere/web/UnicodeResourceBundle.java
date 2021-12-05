package in.drifted.planisphere.web;

import in.drifted.planisphere.l10n.UnicodeControl;
import java.util.Enumeration;
import java.util.ResourceBundle;
import javax.faces.context.FacesContext;

public class UnicodeResourceBundle extends ResourceBundle {

    private static final String BUNDLE_NAME = "in.drifted.planisphere.resources.localizations.messages";
    private static final Control UNICODE_CONTROL = new UnicodeControl();

    public UnicodeResourceBundle() {
        setParent(ResourceBundle.getBundle(BUNDLE_NAME, FacesContext.getCurrentInstance().getViewRoot().getLocale(), UNICODE_CONTROL));
    }

    @Override
    protected Object handleGetObject(String key) {
        return parent.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return parent.getKeys();
    }
}
