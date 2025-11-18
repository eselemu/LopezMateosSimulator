package simulation.ui;

import simulation.TrafficSimulationCore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class ThreadVisualizer extends JFrame implements Runnable {

    private final TrafficSimulationCore simulation;
    private final DefaultTableModel table;
    private volatile Map<Thread.State, Integer> statesMap;
    private final JLabel totalThreadsLabel;

    public ThreadVisualizer() {
        this.simulation = TrafficSimulationCore.getInstance();
        this.statesMap = simulation.getThreadStateCounts();

        setTitle("Visualizador de Estados de Hilos");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Tabla principal ---
        String[] colNames = {"Estado del Thread", "Número de Threads"};
        String[] rowNames = {"Runnable", "Timed Waiting", "Blocked", "Terminated"};
        this.table = new DefaultTableModel(colNames, rowNames.length);
        JTable jTable = new JTable(this.table);
        jTable.setRowHeight(25);

        for (int i = 0; i < rowNames.length; i++) {
            this.table.setValueAt(rowNames[i], i, 0);
        }

        JScrollPane scrollPane = new JScrollPane(jTable);
        add(scrollPane, BorderLayout.CENTER);

        // Parte inferior
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        totalThreadsLabel = new JLabel("Total de hilos activos: 0", SwingConstants.CENTER);
        totalThreadsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(totalThreadsLabel);

        add(infoPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            statesMap = simulation.getThreadStateCounts();

            SwingUtilities.invokeLater(() -> {
                int runnable = statesMap.getOrDefault(Thread.State.RUNNABLE, 0);
                int timedWaiting = statesMap.getOrDefault(Thread.State.TIMED_WAITING, 0);
                int blocked = statesMap.getOrDefault(Thread.State.BLOCKED, 0);
                int terminated = statesMap.getOrDefault(Thread.State.TERMINATED, 0);

                table.setValueAt(runnable, 0, 1);
                table.setValueAt(timedWaiting, 1, 1);
                table.setValueAt(blocked, 2, 1);
                table.setValueAt(terminated, 3, 1);

                // Actualizar total
                int total = runnable + timedWaiting + blocked;
                totalThreadsLabel.setText("Total de hilos activos: " + total);
            });

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                System.out.println("ThreadVisualizer interrupted.");
                Thread.currentThread().interrupt();
            }
        }
    }
}
