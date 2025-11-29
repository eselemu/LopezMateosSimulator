package simulation.ui;

import simulation.TrafficSimulationCore;
import simulation.agents.Car;
import simulation.agents.Pedestrian;
import simulation.agents.SemaphoreSimulation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

import static simulation.agents.Car.CarState.*;

public class ThreadVisualizer extends JFrame implements Runnable {

    private final TrafficSimulationCore simulation;
    private final DefaultTableModel table;
    private final DefaultTableModel carsTable;
    private final DefaultTableModel semaphoresTable;
    private final DefaultTableModel pedestriansTable;
    //private final DefaultTableModel truckTable;
    private volatile Map<Thread.State, Integer> statesMap;
    private volatile Map<Car.CarState, Integer> carStateMap;
    private volatile Map<SemaphoreSimulation.LightState, Integer> semaphoreStateMap;
    private volatile Map<Thread.State, Integer> pedestrianStateMap;
    //    private volatile Map<Truck.TruckState, Integer> truckStateMap;
    private final JLabel totalThreadsLabel;

    public ThreadVisualizer() {
        this.simulation = TrafficSimulationCore.getInstance();
        this.statesMap = simulation.getThreadStateCounts();
        this.carStateMap = simulation.getCarStateCounts();
        this.semaphoreStateMap = simulation.getSemStateCounts();
        this.pedestrianStateMap = simulation.getPedestrianStateCounts();
//        this.truckStateMap = simulation.getTruckStateCounts();

        setTitle("Visualizador de Estados de Hilos");
        setSize(600, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 5, 5)); // 3 rows, 1 column, spacing
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
        mainPanel.add(scrollPane);

        // --- Car Table ---
        String[] carColNames = {"Car State", "Counter"};
        String[] carRowNames = {String.valueOf(Car.CarState.MOVING), String.valueOf(Car.CarState.WAITING),
                String.valueOf(Car.CarState.WAITING_SEMAPHORE), String.valueOf(Car.CarState.IN_INTERSECTION),
                String.valueOf(Car.CarState.FINISHED)};
        this.carsTable = new DefaultTableModel(carColNames, carRowNames.length);
        JTable carJTable = new JTable(this.carsTable);
        carJTable.setRowHeight(25);

        for (int i = 0; i < carRowNames.length; i++) {
            this.carsTable.setValueAt(carRowNames[i], i, 0);
        }

        JScrollPane carScrollPane = new JScrollPane(carJTable);
        mainPanel.add(carScrollPane);

        // --- Semaphore Table --
        String[] semColNames = {"Semaphore State", "Counter"};
        String[] semRowNames = {String.valueOf(SemaphoreSimulation.LightState.RED), String.valueOf((SemaphoreSimulation.LightState.YELLOW)),
                String.valueOf(SemaphoreSimulation.LightState.GREEN)};
        this.semaphoresTable = new DefaultTableModel(semColNames, semRowNames.length);
        JTable semJTable = new JTable(this.semaphoresTable);
        carJTable.setRowHeight(25);

        for (int i = 0; i < semRowNames.length; i++) {
            this.semaphoresTable.setValueAt(semRowNames[i], i, 0);
        }

        JScrollPane semScrollPane = new JScrollPane(semJTable);
        mainPanel.add(semScrollPane);

        // --- Pedestrian Table --
        String[] pedestrianColNames = {"Pedestrian State", "Counter"};
        String[] pedestrianRowNames = {String.valueOf(Thread.State.RUNNABLE), String.valueOf(Thread.State.WAITING),
                String.valueOf(Thread.State.TIMED_WAITING), String.valueOf(Thread.State.BLOCKED), String.valueOf(Thread.State.TERMINATED)};
        this.pedestriansTable = new DefaultTableModel(pedestrianColNames, pedestrianRowNames.length);
        JTable pedestrianJTable = new JTable(this.pedestriansTable);
        pedestrianJTable.setRowHeight(25);

        for (int i = 0; i < pedestrianRowNames.length; i++) {
            this.pedestriansTable.setValueAt(pedestrianRowNames[i], i, 0);
        }

        JScrollPane pedestrianScrollPane = new JScrollPane(pedestrianJTable);
        mainPanel.add(pedestrianScrollPane);

        // --- Truck Table ---
//        String[] truckColNames = {"Truck State", "Counter"};
//        String[] truckRowNames = {String.valueOf(Truck.TruckState.MOVING), String.valueOf(Truck.TruckState.WAITING),
//                String.valueOf(Truck.TruckState.WAITING_SEMAPHORE), String.valueOf(Truck.TruckState.IN_INTERSECTION),
//                String.valueOf(Truck.TruckState.FINISHED)};
//        this.trucksTable = new DefaultTableModel(truckColNames, truckRowNames.length);
//        JTable truckJTable = new JTable(this.trucksTable);
//        truckJTable.setRowHeight(25);
//
//        for (int i = 0; i < truckRowNames.length; i++) {
//            this.trucksTable.setValueAt(truckRowNames[i], i, 0);
//        }
//
//        JScrollPane truckScrollPane = new JScrollPane(truckJTable);
//        mainPanel.add(truckScrollPane);

        // Parte inferior
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        totalThreadsLabel = new JLabel("Total de hilos activos: 0", SwingConstants.CENTER);
        totalThreadsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(totalThreadsLabel);

        add(infoPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);


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

            // --- Car Table ---
            carStateMap = simulation.getCarStateCounts();
            SwingUtilities.invokeLater(() -> {
                int moving = carStateMap.getOrDefault(Car.CarState.MOVING, 0);
                int waiting = carStateMap.getOrDefault(Car.CarState.WAITING, 0);
                int waitingSemaphore = carStateMap.getOrDefault(Car.CarState.WAITING_SEMAPHORE, 0);
                int inIntersection = carStateMap.getOrDefault(Car.CarState.IN_INTERSECTION, 0);
                int finished = carStateMap.getOrDefault(Car.CarState.FINISHED, 0);

                carsTable.setValueAt(moving, 0, 1);
                carsTable.setValueAt(waiting, 1, 1);
                carsTable.setValueAt(waitingSemaphore, 2, 1);
                carsTable.setValueAt(inIntersection, 3, 1);
                carsTable.setValueAt(finished, 4, 1);
            });

            // --- Semaphore Table ---
            semaphoreStateMap = simulation.getSemStateCounts();
            SwingUtilities.invokeLater(() -> {
                int red = semaphoreStateMap.getOrDefault(SemaphoreSimulation.LightState.RED, 0);
                int yellow = semaphoreStateMap.getOrDefault(SemaphoreSimulation.LightState.YELLOW, 0);
                int green = semaphoreStateMap.getOrDefault(SemaphoreSimulation.LightState.GREEN, 0);
                semaphoresTable.setValueAt(red, 0, 1);
                semaphoresTable.setValueAt(yellow, 1, 1);
                semaphoresTable.setValueAt(green, 2, 1);
            });

            // --- Pedestrian Table ---
            pedestrianStateMap = simulation.getPedestrianStateCounts();
            SwingUtilities.invokeLater(() -> {
                int runnable = pedestrianStateMap.getOrDefault(Thread.State.RUNNABLE, 0);
                int waiting = pedestrianStateMap.getOrDefault(Thread.State.WAITING, 0);
                int timedWaiting = pedestrianStateMap.getOrDefault(Thread.State.TIMED_WAITING, 0);
                int blocked = pedestrianStateMap.getOrDefault(Thread.State.BLOCKED, 0);
                int terminated = pedestrianStateMap.getOrDefault(Thread.State.TERMINATED, 0);
                pedestriansTable.setValueAt(runnable, 0, 1);
                pedestriansTable.setValueAt(waiting, 1, 1);
                pedestriansTable.setValueAt(timedWaiting, 2, 1);
                pedestriansTable.setValueAt(blocked, 3, 1);
                pedestriansTable.setValueAt(terminated, 4, 1);
            });

            // --- Truck Table ---
//            truckStateMap = simulation.getTruckStateCounts();
//            SwingUtilities.invokeLater(() -> {
//                int moving = truckStateMap.getOrDefault(Truck.TruckState.MOVING, 0);
//                int waiting = truckStateMap.getOrDefault(Truck.TruckState.WAITING, 0);
//                int waitingSemaphore = truckStateMap.getOrDefault(Truck.TruckState.WAITING_SEMAPHORE, 0);
//                int inIntersection = truckStateMap.getOrDefault(Truck.TruckState.IN_INTERSECTION, 0);
//                int finished = truckStateMap.getOrDefault(Truck.TruckState.FINISHED, 0);
//
//                trucksTable.setValueAt(moving, 0, 1);
//                trucksTable.setValueAt(waiting, 1, 1);
//                trucksTable.setValueAt(waitingSemaphore, 2, 1);
//                trucksTable.setValueAt(inIntersection, 3, 1);
//                trucksTable.setValueAt(finished, 4, 1);
//            });

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                System.out.println("ThreadVisualizer interrupted.");
                Thread.currentThread().interrupt();
            }
        }
    }
}
