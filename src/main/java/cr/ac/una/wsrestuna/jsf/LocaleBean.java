package cr.ac.una.wsrestuna.jsf;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;

import java.io.Serializable;
import java.util.Locale;

@Named("localeBean")
@SessionScoped
public class LocaleBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private Locale locale = new Locale("es", "CR");

    public Locale getLocale() { return locale; }
    public String getLanguage() { return locale.getLanguage(); }

    public void setLanguage(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            locale = Locale.ENGLISH;
        } else {
            locale = new Locale("es", "CR");
        }
        if (FacesContext.getCurrentInstance() != null
                && FacesContext.getCurrentInstance().getViewRoot() != null) {
            FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);
        }
    }
}
