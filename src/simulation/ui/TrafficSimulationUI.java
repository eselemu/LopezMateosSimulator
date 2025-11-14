package simulation.ui;

import simulation.TrafficSimulationCore;
import simulation.agents.Car;
import simulation.agents.SemaphoreSimulation;
import simulation.map.Position;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TrafficSimulationUI extends JFrame {
    private TrafficSimulationCore simulation;
    private SimulationPanel simulationPanel;
    private JTextArea logArea;

    public TrafficSimulationUI(TrafficSimulationCore simulation) {
        this.simulation = simulation;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Simulación de Tráfico - 2 Carros + Semáforo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de simulación
        simulationPanel = new SimulationPanel();
        add(simulationPanel, BorderLayout.CENTER);

        // Panel de logs
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.SOUTH);

        // Panel de controles
        JPanel controlPanel = new JPanel();
        JButton startButton = new JButton("Iniciar");
        JButton stopButton = new JButton("Detener");

        startButton.addActionListener(e -> simulation.startSimulation());
        stopButton.addActionListener(e -> simulation.stopSimulation());

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        add(controlPanel, BorderLayout.NORTH);

        pack();
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Timer para actualizar UI
        Timer timer = new Timer(100, e -> updateUI());
        timer.start();
    }

    private void updateUI() {
        simulationPanel.repaint();
        updateLogs();
    }

    private void updateLogs() {
        // Aquí podrías agregar logs en tiempo real
    }

    private class SimulationPanel extends JPanel {
        private static final int CELL_SIZE = 50;
        private static final int OFFSET = 50;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawMap(g);
            drawAgents(g);
        }

        private void drawMap(Graphics g) {
            g.setColor(Color.LIGHT_GRAY);
            // Dibujar calle horizontal
            g.fillRect(OFFSET, OFFSET, 10 * CELL_SIZE, CELL_SIZE);

            g.setColor(Color.WHITE);
            // Líneas divisorias
            for(int i = 0; i < 10; i++) {
                g.fillRect(OFFSET + i * CELL_SIZE + 20, OFFSET + CELL_SIZE/2 - 2, 10, 4);
            }
        }

        private void drawAgents(Graphics g) {
            List<Car> cars = simulation.getCars();
            List<SemaphoreSimulation> semaphores = simulation.getSemaphores();

            // Dibujar semáforos
            for(SemaphoreSimulation semaphoreSimulation : semaphores) {
                drawSemaphore(g, semaphoreSimulation);
            }

            // Dibujar carros
            for(Car car : cars) {
                drawCar(g, car);
            }
        }

        private void drawSemaphore(Graphics g, SemaphoreSimulation semaphoreSimulation) {
            Position pos = semaphoreSimulation.getPosition();
            int x = OFFSET + pos.x * CELL_SIZE;
            int y = OFFSET;

            // Poste del semáforo
            g.setColor(Color.BLACK);
            g.fillRect(x + 20, y - 20, 5, 20);

            // Caja del semáforo
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x + 15, y - 40, 15, 20);

            // Luz actual
            Color lightColor = switch(semaphoreSimulation.getCurrentState()) {
                case RED -> Color.RED;
                case YELLOW -> Color.YELLOW;
                case GREEN -> Color.GREEN;
            };

            g.setColor(lightColor);
            g.fillOval(x + 18, y - 35, 8, 8);

            // Etiqueta
            g.setColor(Color.BLACK);
            g.drawString("S" + semaphoreSimulation.id, x, y + 20);
        }

        private void drawCar(Graphics g, Car car) {
            Position pos = car.getCurrentPosition();
            int x = OFFSET + pos.x * CELL_SIZE;
            int y = OFFSET;

            // Color según estado
            Color carColor = switch(car.getCarState()) {
                case MOVING -> Color.BLUE;
                case WAITING_AT_SEMAPHORE -> Color.ORANGE;
                case IN_INTERSECTION -> Color.CYAN;
                case FINISHED -> Color.GREEN;
            };

            g.setColor(carColor);
            g.fillRect(x, y + 10, 30, 15);

            // Ventanas
            g.setColor(Color.CYAN);
            g.fillRect(x + 5, y + 12, 8, 8);
            g.fillRect(x + 17, y + 12, 8, 8);

            // Ruedas
            g.setColor(Color.BLACK);
            g.fillOval(x + 3, y + 23, 6, 6);
            g.fillOval(x + 21, y + 23, 6, 6);

            // Etiqueta
            g.setColor(Color.BLACK);
            g.drawString("C" + car.id, x, y + 45);

            // Estado
            g.drawString(car.getCarState().toString(), x, y + 60);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(800, 400);
        }
    }
}