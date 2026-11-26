package com.taller.mecanico.ui;

import atlantafx.base.theme.Styles;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class WcCeldasCarrito {
    private static final int WC_MAXIMO_ABSOLUTO = 9999;

    private WcCeldasCarrito() {
    }

    public static <T> Callback<TableColumn<T, T>, TableCell<T, T>> wcCantidad(
            Function<T, IntegerProperty> wcPropiedad, ToIntFunction<T> wcMaximo, Function<T, String> wcAvisoExceso,
            Consumer<String> wcAviso) {
        return wcCol -> new TableCell<>() {
            private final Button wcMenos = wcBotonIcono(Feather.MINUS);
            private final Button wcMas = wcBotonIcono(Feather.PLUS);
            private final TextField wcValor = new TextField();
            private final HBox wcCaja = new HBox(4, wcMenos, wcValor, wcMas);
            private T wcActual;

            {
                wcCaja.setAlignment(Pos.CENTER_LEFT);
                wcCaja.getStyleClass().add("stepper");
                wcValor.setPrefWidth(52);
                wcValor.setAlignment(Pos.CENTER);
                WcFormularios.wcSoloDigitos(wcValor, 4);
                wcMenos.setOnAction(wcE -> wcCambiar(wcPropiedad.apply(wcActual).get() - 1));
                wcMas.setOnAction(wcE -> wcCambiar(wcPropiedad.apply(wcActual).get() + 1));
                wcValor.setOnAction(wcE -> wcConfirmar());
                wcValor.focusedProperty().addListener((wcObs, wcA, wcEnfocado) -> {
                    if (!wcEnfocado) {
                        wcConfirmar();
                    }
                });
            }

            private int wcLimite() {
                return Math.max(1, Math.min(WC_MAXIMO_ABSOLUTO, wcMaximo.applyAsInt(wcActual)));
            }

            private void wcConfirmar() {
                if (wcActual == null) {
                    return;
                }
                String wcTexto = wcValor.getText();
                wcCambiar(wcTexto == null || wcTexto.isBlank() ? wcPropiedad.apply(wcActual).get() : Integer.parseInt(wcTexto));
            }

            private void wcCambiar(int wcNueva) {
                int wcLimite = wcLimite();
                if (wcNueva > wcLimite && wcLimite < WC_MAXIMO_ABSOLUTO) {
                    wcAviso.accept(wcAvisoExceso.apply(wcActual));
                }
                int wcAcotada = Math.max(1, Math.min(wcLimite, wcNueva));
                wcPropiedad.apply(wcActual).set(wcAcotada);
                wcRefrescar(wcAcotada);
            }

            private void wcRefrescar(int wcCantidad) {
                wcValor.setText(String.valueOf(wcCantidad));
                wcMenos.setDisable(wcCantidad <= 1);
                wcMas.setDisable(wcCantidad >= wcLimite());
            }

            @Override
            protected void updateItem(T wcItem, boolean wcVacio) {
                super.updateItem(wcItem, wcVacio);
                wcActual = wcVacio ? null : wcItem;
                if (wcActual == null) {
                    setGraphic(null);
                    return;
                }
                wcRefrescar(wcPropiedad.apply(wcItem).get());
                setGraphic(wcCaja);
            }
        };
    }

    public static <T> Callback<TableColumn<T, T>, TableCell<T, T>> wcImporte(
            Function<T, ObjectProperty<BigDecimal>> wcPropiedad, Consumer<String> wcAviso) {
        return wcCol -> new TableCell<>() {
            private final TextField wcValor = new TextField();
            private T wcActual;

            {
                wcValor.getStyleClass().add("celda-precio");
                wcValor.setAlignment(Pos.CENTER_RIGHT);
                WcFormularios.wcSoloDecimal(wcValor);
                wcValor.setOnAction(wcE -> wcConfirmar());
                wcValor.focusedProperty().addListener((wcObs, wcA, wcEnfocado) -> {
                    if (!wcEnfocado) {
                        wcConfirmar();
                    }
                });
            }

            private void wcConfirmar() {
                if (wcActual == null) {
                    return;
                }
                try {
                    BigDecimal wcNuevo = new BigDecimal(wcValor.getText().trim().replace(",", "."));
                    if (wcNuevo.signum() <= 0) {
                        throw new NumberFormatException();
                    }
                    wcPropiedad.apply(wcActual).set(wcNuevo.setScale(2, RoundingMode.HALF_UP));
                } catch (NumberFormatException wcE) {
                    wcAviso.accept("Importe invalido; se mantuvo el anterior.");
                }
                wcValor.setText(wcTexto(wcPropiedad.apply(wcActual).get()));
            }

            @Override
            protected void updateItem(T wcItem, boolean wcVacio) {
                super.updateItem(wcItem, wcVacio);
                wcActual = wcVacio ? null : wcItem;
                if (wcActual == null) {
                    setGraphic(null);
                    return;
                }
                wcValor.setText(wcTexto(wcPropiedad.apply(wcItem).get()));
                setGraphic(wcValor);
            }
        };
    }

    public static <T> Callback<TableColumn<T, T>, TableCell<T, T>> wcQuitar(Consumer<T> wcAlQuitar) {
        return wcCol -> new TableCell<>() {
            private final Button wcBoton = wcBotonIcono(Feather.TRASH_2);

            {
                wcBoton.getStyleClass().add(Styles.DANGER);
                wcBoton.setTooltip(new Tooltip("Quitar"));
                wcBoton.setOnAction(wcE -> wcAlQuitar.accept(getItem()));
            }

            @Override
            protected void updateItem(T wcItem, boolean wcVacio) {
                super.updateItem(wcItem, wcVacio);
                setGraphic(wcVacio ? null : wcBoton);
            }
        };
    }

    public static <T> Callback<TableColumn<T, T>, TableCell<T, T>> wcNumeroFila() {
        return wcCol -> new TableCell<>() {
            {
                getStyleClass().add("celda-id");
            }

            @Override
            protected void updateItem(T wcItem, boolean wcVacio) {
                super.updateItem(wcItem, wcVacio);
                setText(wcVacio ? null : String.valueOf(getIndex() + 1));
            }
        };
    }

    private static String wcTexto(BigDecimal wcValor) {
        return wcValor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static Button wcBotonIcono(Feather wcIcono) {
        Button wcB = new Button(null, new FontIcon(wcIcono));
        wcB.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, "boton-mini");
        return wcB;
    }
}
