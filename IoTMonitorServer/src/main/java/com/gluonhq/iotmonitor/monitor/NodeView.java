package com.gluonhq.iotmonitor.monitor;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.chart.ChartData;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.tools.GradientLookup;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Stop;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material.Material;

import java.util.Arrays;

import static com.gluonhq.iotmonitor.monitor.Node.THRESHOLD_PING_TIME;

public class NodeView extends Region {

    private static final int TILE_WIDTH  = 100;
    private static final int TILE_HEIGHT = 100;

    private static final PseudoClass PSEUDO_CLASS_DISCONNECTED = PseudoClass.getPseudoClass("disconnect");

    private Node node;

    private ChartData cpuItem        = new ChartData("CPU", Bright.RED);
    private ChartData memItem        = new ChartData("MEM", Bright.BLUE);
    private GradientLookup gradientLookup = new GradientLookup(Arrays.asList(
            new Stop(0.0, Bright.GREEN), 
            new Stop(0.4, Bright.YELLOW), 
            new Stop(0.8, Bright.RED))
    );

    private Tile cpuMemView = TileBuilder.create()
            .skinType(SkinType.CUSTOM)
            .prefSize(TILE_WIDTH, TILE_HEIGHT)
            .unit("\u0025")
            .title("RESOURCE UTILIZATION")
            .chartData(memItem, cpuItem)
            .build();

    private Tile tempView = TileBuilder.create()
            .skinType(SkinType.BAR_GAUGE)
            .prefSize(TILE_WIDTH, TILE_HEIGHT)
            .minValue(0)
            .maxValue(120)
            .threshold(80)
            .thresholdVisible(true)
            .startFromZero(true)
            .decimals(0)
            .title("TEMPERATURE")
            .unit("C")
            .gradientStops(new Stop(0, Bright.GREEN),
                    new Stop(0.4, Bright.YELLOW),
                    new Stop(0.8, Bright.RED))
            .strokeWithGradient(true)
            .build();

    public NodeView(Node node){
        this.node = node;
        createUI();
    }

    public String getNodeId() {
        return node.getId();
    }

    private void createUI() {
        
        cpuMemView.setSkin(new CpuMemTileSkin(cpuMemView));
        
        HBox upperBox = new HBox(cpuMemView, tempView);
        upperBox.getStyleClass().add("upper-box");

        Label header = new Label("Time since last ping");
        header.getStyleClass().add("header");
        
        Label elapsedTime = new Label(" -- ");
        elapsedTime.getStyleClass().add("elapsed-time");
        
        HBox elapsedPane = new HBox(header, elapsedTime);
        elapsedPane.getStyleClass().add("elapsed-pane");
        HBox.setHgrow(elapsedPane, Priority.ALWAYS);

        Button reboot = new Button();
        reboot.setTooltip(new Tooltip("Reboot"));
        reboot.setGraphic(FontIcon.of(Material.REFRESH, 20));
        reboot.setOnAction((e) -> {
            System.err.println("I have to send reboot request");
            if (node.getProxy() != null) {
                node.getProxy().requestReboot();
                Model.nodeMapper.remove(node.getId());
            } else {
                System.err.println("Could not find proxy for node " + node);
            }
        });
        
        HBox lowerBox = new HBox(elapsedPane, reboot);
        lowerBox.getStyleClass().add("lower-box");

        VBox vbox = new VBox(upperBox, lowerBox);
        vbox.getStyleClass().add("graph-box");

        node.getStat().cpu.addListener((o, ov, nv) -> {
            cpuItem.setFillColor(gradientLookup.getColorAt(nv.doubleValue() / 100.0));
            cpuItem.setValue(nv.doubleValue());
        });
        node.getStat().mem.addListener((o, ov, nv) -> {
            memItem.setFillColor(gradientLookup.getColorAt(nv.doubleValue() / 100.0));
            memItem.setValue(nv.doubleValue());
        });
        tempView.valueProperty().bind(node.getStat().temp);

        elapsedTime.textProperty().bind(node.elapsedTime().asString());

        Label ipLabel = new Label();
        ipLabel.getStyleClass().add("ip-label");
        ipLabel.textProperty().bind(Bindings.concat("Host: ").concat(node.lastKnownIp()));
        Label idLabel = new Label("ID: " + node.getId());
        VBox infoBox = new VBox(idLabel, ipLabel);
        infoBox.getStyleClass().add("info-box");
        BorderPane root = new BorderPane();
        root.getStyleClass().add("container");
        root.setPadding(new Insets(5));
        root.setCenter(vbox);
        root.setTop(infoBox);
        
        getStyleClass().add("node-view");
        getChildren().add(root);

        node.elapsedTime().addListener((obs, ov, nv) -> {
            long elapsed = nv.intValue();
            if (elapsed >= THRESHOLD_PING_TIME) {
                root.pseudoClassStateChanged(PSEUDO_CLASS_DISCONNECTED, true);
            } else {
                root.pseudoClassStateChanged(PSEUDO_CLASS_DISCONNECTED, false);
            }
        });
    }
}
