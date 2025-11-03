package cr.ac.una.wsrestuna.jsf;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named("uiBean")
@SessionScoped
public class UIBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean dark = false;
    public boolean isDark() { return dark; }
    public void toggleTheme() { dark = !dark; }
    public String bodyClass() { return dark ? "dark" : ""; }
}
