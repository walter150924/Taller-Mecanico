package com.taller.mecanico.controller;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.util.Animations;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcUsuario;
import com.taller.mecanico.seguridad.WcSesion;
import com.taller.mecanico.ui.WcCargadorVistas;
import com.taller.mecanico.ui.WcCargadorVistas.WcVista;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcTablas;
import com.taller.mecanico.ui.WcVentana;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WcMainController {
    private static final double WC_ANCHO_EXPANDIDO = 248;
    private static final double WC_ANCHO_CONTRAIDO = 76;
    private static final Duration WC_DURACION_MENU = Duration.millis(260);
    private static final Interpolator WC_SUAVE = Interpolator.SPLINE(0.4, 0, 0.2, 1);

    private final WcCargadorVistas wcCargador;
    private final WcNotificador wcNotificador;
    private final WcSesion wcSesion;
    private final WcVentana wcVentana;

    @FXML private StackPane wcRaiz;
    @FXML private VBox wcSidebar;
    @FXML private StackPane wcContenido;
    @FXML private ModalPane wcModalPanel;
    @FXML private ModalPane wcModalDialogo;
    @FXML private VBox wcZonaNotificaciones;

    @FXML private ToggleGroup wcMenu;
    @FXML private Label wcSecGeneral;
    @FXML private Label wcSecTaller;
    @FXML private Label wcSecVentas;
    @FXML private Label wcSecCompras;
    @FXML private Label wcSecCatalogo;
    @FXML private Label wcSecAdministracion;
    @FXML private ToggleButton wcNavDashboard;
    @FXML private ToggleButton wcNavOrdenes;
    @FXML private ToggleButton wcNavVehiculos;
    @FXML private ToggleButton wcNavClientes;
    @FXML private ToggleButton wcNavPuntoVenta;
    @FXML private ToggleButton wcNavVentas;
    @FXML private ToggleButton wcNavCompra;
    @FXML private ToggleButton wcNavCompras;
    @FXML private ToggleButton wcNavProveedores;
    @FXML private ToggleButton wcNavInventario;
    @FXML private ToggleButton wcNavServicios;
    @FXML private ToggleButton wcNavRepuestos;
    @FXML private ToggleButton wcNavCategorias;
    @FXML private ToggleButton wcNavUsuarios;
    @FXML private Region wcIndicador;

    @FXML private VBox wcMarcaTextos;
    @FXML private VBox wcUsuarioTextos;
    @FXML private Label wcLblIniciales;
    @FXML private Label wcLblUsuario;
    @FXML private Label wcLblRol;
    @FXML private Button wcBtnCerrarSesion;
    @FXML private Button wcBtnTema;
    @FXML private Button wcBtnContraer;

    private final Map<String, WcVista> wcVistas = new HashMap<>();
    private final List<WcOpcionMenu> wcOpcionesPermitidas = new ArrayList<>();
    private final List<Label> wcSeccionesVisibles = new ArrayList<>();
    private boolean wcOscuro;
    private boolean wcContraido;

    private record WcOpcionMenu(ToggleButton wcBoton, String wcVista, WcPermiso wcPermiso) {}

    private record WcSeccion(Label wcTitulo, List<WcOpcionMenu> wcOpciones) {}

    @FXML
    public void initialize() {
        wcVistas.clear();
        wcOscuro = false;
        wcContraido = false;
        wcNotificador.wcRegistrar(wcModalPanel, wcModalDialogo, wcZonaNotificaciones);
        wcModalPanel.setAlignment(Pos.TOP_RIGHT);
        wcModalDialogo.setAlignment(Pos.CENTER);

        wcMostrarUsuario(wcSesion.wcUsuarioActual());
        wcConstruirMenu();

        wcMenu.selectedToggleProperty().addListener((wcObs, wcAnterior, wcActual) -> {
            if (wcActual == null) {
                wcMenu.selectToggle(wcAnterior);
                return;
            }
            wcMostrar((String) wcActual.getUserData());
            wcMoverIndicador(wcActual);
        });

        ToggleButton wcInicial = wcOpcionesPermitidas.get(0).wcBoton();
        wcMenu.selectToggle(wcInicial);
        Platform.runLater(() -> wcMoverIndicador(wcInicial));
    }

    public void wcIrA(String wcVista) {
        wcOpcionesPermitidas.stream()
                .filter(wcOpcion -> wcOpcion.wcVista().equals(wcVista))
                .findFirst()
                .ifPresentOrElse(wcOpcion -> wcMenu.selectToggle(wcOpcion.wcBoton()),
                        () -> wcNotificador.wcAdvertencia("No tienes acceso a esa seccion."));
    }

    private void wcConstruirMenu() {
        List<WcSeccion> wcSecciones = List.of(
                new WcSeccion(wcSecGeneral, List.of(
                        new WcOpcionMenu(wcNavDashboard, "dashboard-view", WcPermiso.WC_VER_DASHBOARD))),
                new WcSeccion(wcSecTaller, List.of(
                        new WcOpcionMenu(wcNavOrdenes, "ordenes-view", WcPermiso.WC_TRABAJAR_ORDENES),
                        new WcOpcionMenu(wcNavVehiculos, "vehiculos-view", WcPermiso.WC_GESTIONAR_VEHICULOS),
                        new WcOpcionMenu(wcNavClientes, "cliente-view", WcPermiso.WC_GESTIONAR_CLIENTES))),
                new WcSeccion(wcSecVentas, List.of(
                        new WcOpcionMenu(wcNavPuntoVenta, "punto-venta-view", WcPermiso.WC_GESTIONAR_VENTAS),
                        new WcOpcionMenu(wcNavVentas, "ventas-view", WcPermiso.WC_GESTIONAR_VENTAS))),
                new WcSeccion(wcSecCompras, List.of(
                        new WcOpcionMenu(wcNavCompra, "compra-form-view", WcPermiso.WC_GESTIONAR_COMPRAS),
                        new WcOpcionMenu(wcNavCompras, "compras-view", WcPermiso.WC_GESTIONAR_COMPRAS),
                        new WcOpcionMenu(wcNavProveedores, "proveedores-view", WcPermiso.WC_GESTIONAR_COMPRAS),
                        new WcOpcionMenu(wcNavInventario, "inventario-view", WcPermiso.WC_VER_INVENTARIO))),
                new WcSeccion(wcSecCatalogo, List.of(
                        new WcOpcionMenu(wcNavServicios, "servicios-view", WcPermiso.WC_GESTIONAR_CATALOGO),
                        new WcOpcionMenu(wcNavRepuestos, "repuesto-view", WcPermiso.WC_GESTIONAR_CATALOGO),
                        new WcOpcionMenu(wcNavCategorias, "categoria-view", WcPermiso.WC_GESTIONAR_CATALOGO))),
                new WcSeccion(wcSecAdministracion, List.of(
                        new WcOpcionMenu(wcNavUsuarios, "usuarios-view", WcPermiso.WC_GESTIONAR_USUARIOS))));

        wcOpcionesPermitidas.clear();
        wcSeccionesVisibles.clear();
        wcSecciones.forEach(this::wcAplicarPermisos);
    }

    private void wcAplicarPermisos(WcSeccion wcSeccion) {
        List<WcOpcionMenu> wcPermitidas = wcSeccion.wcOpciones().stream()
                .filter(wcOpcion -> wcSesion.wcTiene(wcOpcion.wcPermiso()))
                .toList();
        wcSeccion.wcOpciones().stream()
                .filter(wcOpcion -> !wcPermitidas.contains(wcOpcion))
                .forEach(wcOpcion -> wcQuitarDelMenu(wcOpcion.wcBoton()));
        if (wcPermitidas.isEmpty()) {
            wcQuitarDelMenu(wcSeccion.wcTitulo());
            return;
        }
        wcSeccionesVisibles.add(wcSeccion.wcTitulo());
        wcPermitidas.forEach(this::wcPrepararOpcion);
        wcOpcionesPermitidas.addAll(wcPermitidas);
    }

    private void wcPrepararOpcion(WcOpcionMenu wcOpcion) {
        wcOpcion.wcBoton().setUserData(wcOpcion.wcVista());
        Tooltip wcAyuda = new Tooltip(wcOpcion.wcBoton().getText());
        wcAyuda.setShowDelay(Duration.millis(300));
        wcOpcion.wcBoton().setTooltip(wcAyuda);
    }

    private static void wcQuitarDelMenu(Node wcNodo) {
        ((Pane) wcNodo.getParent()).getChildren().remove(wcNodo);
    }

    private void wcMostrarUsuario(WcUsuario wcUsuario) {
        wcLblIniciales.setText(WcTablas.wcIniciales(wcUsuario.getWcNombreCompleto()));
        wcLblUsuario.setText(wcUsuario.getWcNombreCompleto());
        wcLblRol.setText(wcUsuario.getWcRol().getWcDescripcion());
    }

    private void wcMostrar(String wcNombre) {
        WcVista wcVista = wcVistas.computeIfAbsent(wcNombre, wcCargador::wcCargar);
        wcContenido.getChildren().setAll(wcVista.wcNodo());
        if (wcVista.wcControlador() instanceof WcRefrescable wcRefrescable) {
            wcRefrescable.wcRefrescar();
        }
        Animations.fadeInUp(wcVista.wcNodo(), Duration.millis(380)).play();
    }

    private void wcMoverIndicador(Toggle wcToggle) {
        Node wcBoton = (Node) wcToggle;
        double wcDestino = wcBoton.getBoundsInParent().getMinY();
        wcIndicador.setPrefHeight(wcBoton.getBoundsInParent().getHeight());
        new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(wcIndicador.translateYProperty(), wcDestino, WC_SUAVE)))
                .play();
    }

    @FXML
    private void wcOnCerrarSesion() {
        wcNotificador.wcConfirmar("¿Cerrar sesion?",
                "Volveras a la pantalla de ingreso. Los cambios ya guardados se conservan.",
                "Cerrar sesion",
                () -> {
                    wcSesion.wcCerrar();
                    wcVentana.wcMostrarLogin();
                });
    }

    @FXML
    private void wcOnCambiarTema() {
        wcOscuro = !wcOscuro;
        Application.setUserAgentStylesheet(wcOscuro
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        if (wcOscuro) {
            wcRaiz.getStyleClass().add("dark");
        } else {
            wcRaiz.getStyleClass().remove("dark");
        }
        ((FontIcon) wcBtnTema.getGraphic()).setIconCode(wcOscuro ? Feather.SUN : Feather.MOON);
        wcBtnTema.setText(wcOscuro ? "Modo claro" : "Modo oscuro");
        Animations.fadeIn(wcContenido, Duration.millis(300)).play();
    }

    @FXML
    private void wcOnContraerMenu() {
        wcContraido = !wcContraido;
        double wcAncho = wcContraido ? WC_ANCHO_CONTRAIDO : WC_ANCHO_EXPANDIDO;

        Timeline wcAjuste = new Timeline(new KeyFrame(WC_DURACION_MENU,
                new KeyValue(wcSidebar.prefWidthProperty(), wcAncho, WC_SUAVE),
                new KeyValue(wcSidebar.minWidthProperty(), wcAncho, WC_SUAVE)));

        ContentDisplay wcModo = wcContraido ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT;
        List<Node> wcTextos = new ArrayList<>(wcSeccionesVisibles);
        wcTextos.add(wcMarcaTextos);
        wcTextos.add(wcUsuarioTextos);

        if (wcContraido) {
            wcTextos.forEach(wcNodo -> wcAlternarVisibilidad(wcNodo, false));
            wcTodosLosBotones().forEach(wcBoton -> wcBoton.setContentDisplay(wcModo));
            wcAjuste.play();
        } else {
            wcAjuste.setOnFinished(wcE -> {
                wcTextos.forEach(wcNodo -> wcAlternarVisibilidad(wcNodo, true));
                wcTodosLosBotones().forEach(wcBoton -> wcBoton.setContentDisplay(wcModo));
                ParallelTransition wcAparicion = new ParallelTransition();
                wcTextos.forEach(wcNodo -> wcAparicion.getChildren().add(Animations.fadeIn(wcNodo, Duration.millis(200))));
                wcAparicion.play();
                Platform.runLater(() -> wcMoverIndicador(wcMenu.getSelectedToggle()));
            });
            wcAjuste.play();
        }
        ((FontIcon) wcBtnContraer.getGraphic()).setIconCode(
                wcContraido ? Feather.CHEVRONS_RIGHT : Feather.CHEVRONS_LEFT);
        Platform.runLater(() -> wcMoverIndicador(wcMenu.getSelectedToggle()));
    }

    private static void wcAlternarVisibilidad(Node wcNodo, boolean wcVisible) {
        wcNodo.setVisible(wcVisible);
        wcNodo.setManaged(wcVisible);
    }

    private List<ButtonBase> wcTodosLosBotones() {
        List<ButtonBase> wcTodos = new ArrayList<>(wcOpcionesPermitidas.stream().map(WcOpcionMenu::wcBoton).toList());
        wcTodos.add(wcBtnCerrarSesion);
        wcTodos.add(wcBtnTema);
        wcTodos.add(wcBtnContraer);
        return wcTodos;
    }
}
