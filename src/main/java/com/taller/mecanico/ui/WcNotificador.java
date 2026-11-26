package com.taller.mecanico.ui;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Component
public class WcNotificador {
    private static final Duration WC_DURACION_TOAST = Duration.seconds(3.5);

    private ModalPane wcModalPanel;
    private ModalPane wcModalDialogo;
    private VBox wcZonaToasts;

    public void wcRegistrar(ModalPane wcModalPanel, ModalPane wcModalDialogo, VBox wcZonaToasts) {
        this.wcModalPanel = wcModalPanel;
        this.wcModalDialogo = wcModalDialogo;
        this.wcZonaToasts = wcZonaToasts;
    }

    public void wcExito(String wcMensaje) {
        wcToast(wcMensaje, Feather.CHECK_CIRCLE, Styles.SUCCESS);
    }

    public void wcAdvertencia(String wcMensaje) {
        wcToast(wcMensaje, Feather.ALERT_TRIANGLE, Styles.WARNING);
    }

    public void wcError(String wcMensaje) {
        wcToast(wcMensaje, Feather.X_CIRCLE, Styles.DANGER);
    }

    public void wcInfo(String wcMensaje) {
        wcToast(wcMensaje, Feather.INFO, Styles.ACCENT);
    }

    private void wcToast(String wcMensaje, Ikon wcIcono, String wcEstilo) {
        Notification wcN = new Notification(wcMensaje, new FontIcon(wcIcono));
        wcN.getStyleClass().addAll(wcEstilo, Styles.ELEVATED_2, "toast");
        wcN.setPrefWidth(360);
        wcN.setMaxWidth(360);
        wcN.setOnClose(wcE -> wcCerrarToast(wcN));

        wcZonaToasts.getChildren().add(wcN);
        Animations.fadeInUp(wcN, Duration.millis(280)).play();

        PauseTransition wcEspera = new PauseTransition(WC_DURACION_TOAST);
        wcEspera.setOnFinished(wcE -> wcCerrarToast(wcN));
        wcEspera.play();
    }

    private void wcCerrarToast(Notification wcN) {
        if (!wcZonaToasts.getChildren().contains(wcN) || wcN.getProperties().containsKey("cerrando")) {
            return;
        }
        wcN.getProperties().put("cerrando", true);
        Timeline wcSalida = Animations.fadeOutRight(wcN, Duration.millis(250));
        wcSalida.setOnFinished(wcE -> wcZonaToasts.getChildren().remove(wcN));
        wcSalida.play();
    }

    public void wcMostrarDialogo(Node wcContenido) {
        wcModalDialogo.usePredefinedTransitionFactories(null);
        wcModalDialogo.show(wcContenido);
    }

    public void wcCerrarDialogo() {
        wcModalDialogo.hide();
    }

    public void wcConfirmar(String wcTitulo, String wcMensaje, String wcTextoAccion, Runnable wcAlConfirmar) {
        wcDialogo(Feather.TRASH_2, "peligro", wcTitulo, wcMensaje,
                "Cancelar", null, wcTextoAccion, Styles.DANGER, wcAlConfirmar);
    }

    public void wcExitoConAcciones(String wcTitulo, String wcMensaje,
                                 String wcTextoSecundario, Runnable wcSecundaria,
                                 String wcTextoPrincipal, Runnable wcPrincipal) {
        wcDialogo(Feather.CHECK_CIRCLE, "exito", wcTitulo, wcMensaje,
                wcTextoSecundario, wcSecundaria, wcTextoPrincipal, Styles.ACCENT, wcPrincipal);
    }

    private void wcDialogo(Ikon wcIconoDialogo, String wcClaseIcono, String wcTitulo, String wcMensaje,
                         String wcTextoSecundario, Runnable wcSecundaria,
                         String wcTextoPrincipal, String wcEstiloPrincipal, Runnable wcPrincipal) {
        FontIcon wcIcono = new FontIcon(wcIconoDialogo);
        StackPane wcCirculo = new StackPane(wcIcono);
        wcCirculo.getStyleClass().addAll("dialogo-icono", wcClaseIcono);

        Label wcLblTitulo = new Label(wcTitulo);
        wcLblTitulo.getStyleClass().add("dialogo-titulo");
        Label wcLblMensaje = new Label(wcMensaje);
        wcLblMensaje.getStyleClass().add("dialogo-mensaje");
        wcLblMensaje.setWrapText(true);

        Button wcCancelar = new Button(wcTextoSecundario);
        wcCancelar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        wcCancelar.setCancelButton(true);
        wcCancelar.setOnAction(wcE -> {
            wcModalDialogo.hide();
            if (wcSecundaria != null) {
                wcSecundaria.run();
            }
        });

        Button wcAceptar = new Button(wcTextoPrincipal);
        wcAceptar.getStyleClass().add(wcEstiloPrincipal);
        wcAceptar.setOnAction(wcE -> {
            wcModalDialogo.hide();
            wcPrincipal.run();
        });

        Region wcEspacio = new Region();
        HBox.setHgrow(wcEspacio, javafx.scene.layout.Priority.ALWAYS);
        HBox wcAcciones = new HBox(10, wcEspacio, wcCancelar, wcAceptar);
        wcAcciones.setAlignment(Pos.CENTER_RIGHT);
        wcAcciones.getStyleClass().add("dialogo-acciones");

        VBox wcDialogo = new VBox(14, wcCirculo, wcLblTitulo, wcLblMensaje, wcAcciones);
        wcDialogo.getStyleClass().add("dialogo");
        wcDialogo.setMaxSize(440, Region.USE_PREF_SIZE);

        wcModalDialogo.usePredefinedTransitionFactories(null);
        wcModalDialogo.show(wcDialogo);
        wcAceptar.requestFocus();
    }

    public void wcAbrirPanel(Node wcContenido) {
        wcModalPanel.usePredefinedTransitionFactories(Side.RIGHT,
                Duration.millis(320), Duration.millis(220));
        wcModalPanel.show(wcContenido);
    }

    public void wcCerrarPanel() {
        wcModalPanel.hide();
    }
}
