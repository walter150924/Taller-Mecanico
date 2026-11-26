package com.taller.mecanico.ui;

import com.taller.mecanico.model.WcEstadoOrden;
import com.taller.mecanico.model.WcRepuesto;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

public final class WcTablas {
    public static final Locale WC_LOCALE = Locale.forLanguageTag("es-PE");
    public static final NumberFormat WC_MONEDA = NumberFormat.getCurrencyInstance(WC_LOCALE);

    private WcTablas() {
    }

    public static <T> void wcConfigurar(TableView<T> wcTabla, String wcTextoVacio, Consumer<T> wcAlDobleClic) {
        wcConfigurar(wcTabla, Feather.INBOX, "Sin resultados", wcTextoVacio, wcAlDobleClic);
    }

    public static <T> void wcConfigurar(TableView<T> wcTabla, Ikon wcIconoVacio, String wcTituloVacio, String wcTextoVacio,
                                      Consumer<T> wcAlDobleClic) {
        wcTabla.getStyleClass().add("tabla");
        wcTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        FontIcon wcIcono = new FontIcon(wcIconoVacio);
        wcIcono.getStyleClass().add("vacio-icono");
        Label wcTitulo = new Label(wcTituloVacio);
        wcTitulo.getStyleClass().add("vacio-titulo");
        Label wcDetalle = new Label(wcTextoVacio);
        wcDetalle.getStyleClass().add("vacio-detalle");
        VBox wcVacio = new VBox(8, wcIcono, wcTitulo, wcDetalle);
        wcVacio.setAlignment(Pos.CENTER);
        wcTabla.setPlaceholder(wcVacio);

        wcTabla.setRowFactory(wcTv -> {
            TableRow<T> wcFila = new TableRow<>();
            wcFila.setOnMouseClicked(wcE -> {
                if (wcE.getClickCount() == 2 && !wcFila.isEmpty()) {
                    wcAlDobleClic.accept(wcFila.getItem());
                }
            });
            return wcFila;
        });
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> wcCeldaId() {
        return wcCol -> new TableCell<>() {
            {
                getStyleClass().add("celda-id");
            }

            @Override
            protected void updateItem(String wcValor, boolean wcVacio) {
                super.updateItem(wcValor, wcVacio);
                setText(wcVacio || wcValor == null ? null : "#" + wcValor);
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> wcCeldaAvatar() {
        return wcCol -> new TableCell<>() {
            private final Label wcIniciales = new Label();
            private final StackPane wcAvatar = new StackPane(wcIniciales);
            private final Label wcNombre = new Label();
            private final HBox wcCaja = new HBox(10, wcAvatar, wcNombre);

            {
                wcAvatar.getStyleClass().add("avatar");
                wcCaja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String wcValor, boolean wcVacio) {
                super.updateItem(wcValor, wcVacio);
                if (wcVacio || wcValor == null) {
                    setGraphic(null);
                    return;
                }
                wcIniciales.setText(wcIniciales(wcValor));
                wcNombre.setText(wcValor);
                setGraphic(wcCaja);
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> wcCeldaChip(String wcTextoVacio) {
        return wcCol -> new TableCell<>() {
            private final Label wcChip = new Label();

            @Override
            protected void updateItem(String wcValor, boolean wcVacio) {
                super.updateItem(wcValor, wcVacio);
                if (wcVacio) {
                    setGraphic(null);
                    return;
                }
                boolean wcSinValor = wcValor == null || wcValor.isBlank();
                wcChip.setText(wcSinValor ? wcTextoVacio : wcValor);
                wcChip.getStyleClass().setAll("chip");
                if (wcSinValor) {
                    wcChip.getStyleClass().add("neutro");
                }
                setGraphic(wcChip);
            }
        };
    }

    public static <T> Callback<TableColumn<T, BigDecimal>, TableCell<T, BigDecimal>> wcCeldaMoneda() {
        return wcCol -> new TableCell<>() {
            {
                getStyleClass().add("celda-importe");
            }

            @Override
            protected void updateItem(BigDecimal wcValor, boolean wcVacio) {
                super.updateItem(wcValor, wcVacio);
                setText(wcVacio || wcValor == null ? null : WC_MONEDA.format(wcValor));
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> wcCeldaOpcional() {
        return wcCol -> new TableCell<>() {
            @Override
            protected void updateItem(String wcValor, boolean wcVacio) {
                super.updateItem(wcValor, wcVacio);
                boolean wcSinValor = !wcVacio && (wcValor == null || wcValor.isBlank());
                setText(wcVacio ? null : wcSinValor ? "—" : wcValor);
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("sin-valor"), wcSinValor);
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> wcCeldaEstado() {
        return wcCol -> new TableCell<>() {
            private final Label wcChip = new Label();

            @Override
            protected void updateItem(String wcValor, boolean wcVacio) {
                super.updateItem(wcValor, wcVacio);
                if (wcVacio || wcValor == null) {
                    setGraphic(null);
                    return;
                }
                wcChip.setText(wcValor);
                wcChip.getStyleClass().setAll("chip", "ANULADA".equals(wcValor) ? "peligro" : "exito");
                setGraphic(wcChip);
            }
        };
    }

    public static <T> Callback<TableColumn<T, WcRepuesto>, TableCell<T, WcRepuesto>> wcCeldaStock() {
        return wcCol -> new TableCell<>() {
            private final Label wcChip = new Label();

            @Override
            protected void updateItem(WcRepuesto wcP, boolean wcVacio) {
                super.updateItem(wcP, wcVacio);
                if (wcVacio || wcP == null) {
                    setGraphic(null);
                    return;
                }
                wcChip.setText(wcP.isWcSinStock() ? "Sin stock" : wcP.getWcStock() + " u.");
                wcChip.getStyleClass().setAll("chip", wcClaseStock(wcP));
                setGraphic(wcChip);
            }
        };
    }

    public static String wcClaseStock(WcRepuesto wcP) {
        return wcP.isWcSinStock() ? "peligro" : wcP.isWcStockBajo() ? "advertencia" : "exito";
    }

    public static <T> Callback<TableColumn<T, WcEstadoOrden>, TableCell<T, WcEstadoOrden>> wcCeldaEstadoOrden() {
        return wcCol -> new TableCell<>() {
            private final Label wcChip = new Label();

            @Override
            protected void updateItem(WcEstadoOrden wcEstado, boolean wcVacio) {
                super.updateItem(wcEstado, wcVacio);
                if (wcVacio || wcEstado == null) {
                    setGraphic(null);
                    return;
                }
                wcAplicarEstilo(wcChip, wcEstado);
                setGraphic(wcChip);
            }
        };
    }

    public static void wcAplicarEstilo(Label wcChip, WcEstadoOrden wcEstado) {
        wcChip.setText(wcEstado.getWcDescripcion());
        wcChip.getStyleClass().setAll("chip", wcClaseEstado(wcEstado));
    }

    public static String wcClaseEstado(WcEstadoOrden wcEstado) {
        return switch (wcEstado) {
            case WC_RECIBIDO -> "neutro";
            case WC_EN_DIAGNOSTICO, WC_EN_REPARACION -> "advertencia";
            case WC_LISTO -> "listo";
            case WC_ENTREGADO -> "exito";
            case WC_CANCELADO -> "peligro";
        };
    }

    public static <T> Callback<TableColumn<T, Boolean>, TableCell<T, Boolean>> wcCeldaActivo() {
        return wcCol -> new TableCell<>() {
            private final Label wcChip = new Label();

            @Override
            protected void updateItem(Boolean wcActivo, boolean wcVacio) {
                super.updateItem(wcActivo, wcVacio);
                if (wcVacio || wcActivo == null) {
                    setGraphic(null);
                    return;
                }
                wcChip.setText(wcActivo ? "Activo" : "Inactivo");
                wcChip.getStyleClass().setAll("chip", wcActivo ? "exito" : "neutro");
                setGraphic(wcChip);
            }
        };
    }

    public static String wcIniciales(String wcTexto) {
        String[] wcPartes = wcTexto.trim().split("\\s+");
        StringBuilder wcSb = new StringBuilder();
        for (int wcI = 0; wcI < wcPartes.length && wcSb.length() < 2; wcI++) {
            if (!wcPartes[wcI].isEmpty()) {
                wcSb.append(Character.toUpperCase(wcPartes[wcI].charAt(0)));
            }
        }
        return wcSb.toString();
    }
}
