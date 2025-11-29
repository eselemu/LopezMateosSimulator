package simulation.ui;

import simulation.TrafficSimulationCore;
import simulation.agents.Agent;
import simulation.map.MapManager;
import simulation.map.StreetSegment;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentVisualizer extends JFrame implements Runnable {
    private final TrafficSimulationCore simulation;
    private final MapManager mapManager;
    private Map<String, Integer> agentCount;
    private Map<StreetSegment, Integer> segmentCount;
    private final DefaultTableModel table;

    public AgentVisualizer() {
        this.simulation = TrafficSimulationCore.getInstance();
        this.mapManager = MapManager.getInstance();
        this.agentCount = simulation.getAgentCount();
        this.segmentCount = mapManager.getStreetSegments();

        setTitle("Visualizador de Agentes, Buffers y Zonas Críticas");
        setSize(600, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] colNames = {"Objeto", "Número"};
        this.table = new DefaultTableModel(colNames, 0); // 0 filas iniciales
        JTable jTable = new JTable(this.table);
        jTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(jTable);
        add(scrollPane, BorderLayout.CENTER);




        setVisible(true);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            agentCount = simulation.getAgentCount();
            segmentCount = mapManager.getStreetSegments();

            SwingUtilities.invokeLater(() -> {
                // Limpia la tabla completamente antes de reconstruirla
                table.setRowCount(0);

                // Agentes
                table.addRow(new Object[]{"Agent Car", "Existen " + agentCount.getOrDefault("Car", 0) + " agentes"});
                table.addRow(new Object[]{"Agent Semaphore", "Existen " + agentCount.getOrDefault("Semaphore", 0) + " agentes"});
                table.addRow(new Object[]{"Agent Pedestrian", "Existen " + agentCount.getOrDefault("Pedestrian", 0) + " agentes"});

                //Buffers y zonas critica
                int bufferIndex = 0;
                for (StreetSegment segment : segmentCount.keySet()) {
                    int occupancy = segmentCount.get(segment);

                    // Buffer
                    table.addRow(new Object[]{
                            "Buffer <Segmento " + bufferIndex + ">",
                            occupancy + " / " + 1 + " ocupado(s)"
                    });

                    // Zona crítica (solo si está ocupada)
                    if (segment.getCurrentOccupancy() > 0) {
                        table.addRow(new Object[]{
                                "Zona crítica <Segmento " + bufferIndex + ">",
                                "UN AGENTE EN CS"
                        });
                    } else {
                        table.addRow(new Object[]{
                                "Zona crítica <Segmento " + bufferIndex + ">",
                                "-"
                        });
                    }

                    bufferIndex++;
                }
            });

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                System.out.println("AgentVisualizer interrupted.");
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }
}
