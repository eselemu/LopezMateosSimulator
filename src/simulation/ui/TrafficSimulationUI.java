package simulation.ui;

import simulation.TrafficSimulationCore;
import simulation.agents.Car;
import simulation.agents.SemaphoreSimulation;
import simulation.agents.Truck;
import simulation.map.Position;
import simulation.map.TrafficEdge;
import simulation.map.TrafficMap;
import simulation.map.TrafficNode;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TrafficSimulationUI extends JFrame {
    private TrafficSimulationCore simulation;
    private SimulationPanel simulationPanel;
    private JTextArea logArea;
    private int carsNumber;
    private int semaphoresNumber;
    private int pedestriansNumber;
    private int greenTimerSemInput;
    private int yellowTimerSemInput;
    private int redTimerSemInput;

    public TrafficSimulationUI(TrafficSimulationCore simulation) {
        this.simulation = simulation;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Simulación de Tráfico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Simulation Panel
        simulationPanel = new SimulationPanel();
        add(simulationPanel, BorderLayout.CENTER);

        // Logs
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.SOUTH);

        // Inputs
        JLabel carLabel = new JLabel("Carros: ");
        JTextField carNumber = new JTextField("0", 3);

        JLabel semaphoreLabel = new JLabel("Camiones: ");
        JTextField semaphoreNumber = new JTextField("0", 3);

        JLabel pedestrianLabel = new JLabel("Peatones: ");
        JTextField pedestrianNumber = new JTextField("0", 3);

        JLabel greenTimerLabel = new JLabel("Green Light Timer (s): ");
        JTextField greenTimerInput = new JTextField("2", 3);

        JLabel yellowTimerLabel = new JLabel("Yellow Light Timer (s): ");
        JTextField yellowTimerInput = new JTextField("1", 3);

        JLabel redTimerLabel = new JLabel("Red Light Timer (s): ");
        JTextField redTimerInput = new JTextField("5", 3);

        JButton startButton = new JButton("Iniciar");
        JButton stopButton = new JButton("Detener");

        // --- Panel superior ---
        JPanel controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(this.getWidth(), 100));
        controlPanel.add(carLabel);
        controlPanel.add(carNumber);
        controlPanel.add(semaphoreLabel);
        controlPanel.add(semaphoreNumber);
        controlPanel.add(pedestrianLabel);
        controlPanel.add(pedestrianNumber);

        controlPanel.add(greenTimerLabel);
        controlPanel.add(greenTimerInput);
        controlPanel.add(yellowTimerLabel);
        controlPanel.add(yellowTimerInput);
        controlPanel.add(redTimerLabel);
        controlPanel.add(redTimerInput);

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        add(controlPanel, BorderLayout.NORTH);

        stopButton.setEnabled(false);

        // Button Action
        startButton.addActionListener(e -> {
            String carInput = carNumber.getText();
            String semInput = semaphoreNumber.getText();
            String pedInput = pedestrianNumber.getText();
            String greenSemInput = greenTimerInput.getText();
            String yellowSemInput = yellowTimerInput.getText();
            String redSemInput = redTimerInput.getText();

            // Validate inputs before starting simulation
            if (!isValidNumber(carInput) || !isValidNumber(semInput) || !isValidNumber(pedInput) || !isValidNumber(greenSemInput) || !isValidNumber(yellowSemInput) || !isValidNumber(redSemInput)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Por favor ingresa valores numéricos entre 0 y 9 para todos los campos.",
                        "Entrada inválida",
                        JOptionPane.WARNING_MESSAGE
                );
                return; // Stop here
            }

            setInputs(Integer.parseInt(carInput), Integer.parseInt(semInput), Integer.parseInt(pedInput), Integer.parseInt(greenSemInput), Integer.parseInt(yellowSemInput), Integer.parseInt(redSemInput));

            // Initialize and start simulation
            simulation.initializeSimulation(getInputs()[0], getInputs()[1], 0, getInputs()[2], getInputs()[3], getInputs()[4], getInputs()[5]);
            simulation.startSimulation();

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        });

        stopButton.addActionListener(e -> {
            simulation.stopSimulation();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });

        pack();
        setSize(850, 600);
        setLocationRelativeTo(null);

        Timer timer = new Timer(100, e -> updateUI());
        timer.start();

        setVisible(true);
    }

    public void setInputs(int numCars, int numSemaphores, int numPedestrians, int greenTimerSemInput, int yellowTimerSemInput, int redTimerSemInput){
        this.carsNumber = numCars;
        this.semaphoresNumber = numSemaphores;
        this.pedestriansNumber = numPedestrians;
        this.greenTimerSemInput = greenTimerSemInput;
        this.yellowTimerSemInput = yellowTimerSemInput;
        this.redTimerSemInput = redTimerSemInput;
    }

    public int[] getInputs(){
        return new int[]{carsNumber, semaphoresNumber, pedestriansNumber, greenTimerSemInput, yellowTimerSemInput, redTimerSemInput};
    }

    private void updateUI() {
        simulationPanel.repaint();
        updateLogs();
    }

    private void updateLogs() {
        // Aquí podrías agregar logs en tiempo real
    }

    private class SimulationPanel extends JPanel {
        private static final int CELL_SIZE = 40; // Smaller cells for grid
        private static final int OFFSET = 50;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawMap(g);
            drawAgents(g);
        }

        private void drawMap(Graphics g) {
            TrafficMap trafficMap = simulation.getMapManager().getTrafficMap();
            int scale = trafficMap.getScale();

            // Draw streets (light gray background)
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(OFFSET, OFFSET, trafficMap.getWidth() * CELL_SIZE, trafficMap.getHeight() * CELL_SIZE);

            // Draw grid lines and streets
            g.setColor(Color.DARK_GRAY);
            for (int x = 0; x <= trafficMap.getWidth(); x++) {
                g.drawLine(OFFSET + x * CELL_SIZE, OFFSET, OFFSET + x * CELL_SIZE,
                        OFFSET + trafficMap.getHeight() * CELL_SIZE);
            }
            for (int y = 0; y <= trafficMap.getHeight(); y++) {
                g.drawLine(OFFSET, OFFSET + y * CELL_SIZE,
                        OFFSET + trafficMap.getWidth() * CELL_SIZE, OFFSET + y * CELL_SIZE);
            }

            // Draw intersections (dark gray circles)
            g.setColor(Color.DARK_GRAY);
            for (TrafficNode node : trafficMap.getNodes().values()) {
                if (node.getType() == TrafficNode.NodeType.INTERSECTION) {
                    int x = OFFSET + (node.position.x / scale) * CELL_SIZE;
                    int y = OFFSET + (node.position.y / scale) * CELL_SIZE;
                    g.fillOval(x - 3, y - 3, 6, 6);
                }
            }

            // Draw directed edges (arrows)
            g.setColor(Color.BLUE);
            for (TrafficEdge edge : trafficMap.getEdges().values()) {
                Position fromPos = edge.getFrom().position;
                Position toPos = edge.getTo().position;

                int fromX = OFFSET + (fromPos.x / scale) * CELL_SIZE;
                int fromY = OFFSET + (fromPos.y / scale) * CELL_SIZE;
                int toX = OFFSET + (toPos.x / scale) * CELL_SIZE;
                int toY = OFFSET + (toPos.y / scale) * CELL_SIZE;

                // Draw arrow line
                g.drawLine(fromX, fromY, toX, toY);

                // Draw arrow head
                drawArrow(g, fromX, fromY, toX, toY);
            }
        }

        private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowLength = 10;

            int x3 = (int) (x2 - arrowLength * Math.cos(angle - Math.PI / 6));
            int y3 = (int) (y2 - arrowLength * Math.sin(angle - Math.PI / 6));
            int x4 = (int) (x2 - arrowLength * Math.cos(angle + Math.PI / 6));
            int y4 = (int) (y2 - arrowLength * Math.sin(angle + Math.PI / 6));

            g.drawLine(x2, y2, x3, y3);
            g.drawLine(x2, y2, x4, y4);
        }

        private void drawAgents(Graphics g) {
            List<Car> cars = simulation.getCars();
            List<Truck> trucks = simulation.getTrucks();
            List<SemaphoreSimulation> semaphores = simulation.getSemaphores();

            TrafficMap trafficMap = simulation.getMapManager().getTrafficMap();
            int scale = trafficMap.getScale();

            // Draw semáforos
            for(SemaphoreSimulation semaphoreSimulation : semaphores) {
                drawSemaphore(g, semaphoreSimulation, trafficMap, scale);
            }

            // Draw trucks
            for(Truck truck : trucks) {
                drawTruck(g, truck, trafficMap, scale);
            }

            // Draw cars
            for(Car car : cars) {
                drawCar(g, car, trafficMap, scale);
            }
        }

        private void drawSemaphore(Graphics g, SemaphoreSimulation semaphore, TrafficMap trafficMap, int scale) {
            Position pos = semaphore.getPosition();
            int x = OFFSET + (pos.x / scale) * CELL_SIZE;
            int y = OFFSET + (pos.y / scale) * CELL_SIZE;

            // Draw semaphore pole
            g.setColor(Color.BLACK);
            g.fillRect(x - 1, y - 15, 2, 15);

            // Draw semaphore box
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x - 5, y - 25, 10, 10);

            // Draw current light
            Color lightColor = switch(semaphore.getCurrentState()) {
                case RED -> Color.RED;
                case YELLOW -> Color.YELLOW;
                case GREEN -> Color.GREEN;
            };
            g.setColor(lightColor);
            g.fillOval(x - 3, y - 23, 6, 6);
        }

        private void drawTruck(Graphics g, Truck truck, TrafficMap trafficMap, int scale) {
            Position frontPos = truck.getCurrentPosition();
            Position rearPos = truck.getRearPosition();

            int frontX = OFFSET + (frontPos.x / scale) * CELL_SIZE;
            int frontY = OFFSET + (frontPos.y / scale) * CELL_SIZE;
            int rearX = OFFSET + (rearPos.x / scale) * CELL_SIZE;
            int rearY = OFFSET + (rearPos.y / scale) * CELL_SIZE;

            // Color según estado
            Color truckColor = switch(truck.getTruckState()) {
                case MOVING -> new Color(139, 69, 19); // Brown
                case WAITING -> Color.ORANGE;
                case WAITING_SEMAPHORE -> Color.RED;
                case WAITING_DOUBLE_NODE -> Color.MAGENTA;
                case FINISHED -> Color.GREEN;
            };

            g.setColor(truckColor);

            // Draw truck as a rectangle between front and rear positions
            int truckX = Math.min(frontX, rearX);
            int truckY = Math.min(frontY, rearY);
            int truckWidth = Math.abs(frontX - rearX) + 8;
            int truckHeight = Math.abs(frontY - rearY) + 8;

            g.fillRect(truckX - 4, truckY - 4, truckWidth, truckHeight);

            // Draw truck label
            g.setColor(Color.WHITE);
            g.drawString("T" + truck.id, truckX + truckWidth/2 - 5, truckY + truckHeight/2);
        }

        private void drawCar(Graphics g, Car car, TrafficMap trafficMap, int scale) {
            Position pos = car.getCurrentPosition();
            int x = OFFSET + (pos.x / scale) * CELL_SIZE;
            int y = OFFSET + (pos.y / scale) * CELL_SIZE;

            // Color según estado
            Color carColor = switch(car.getCarState()) {
                case MOVING -> Color.BLUE;
                case WAITING -> Color.ORANGE;
                case WAITING_SEMAPHORE -> Color.RED;
                case IN_INTERSECTION -> Color.CYAN;
                case FINISHED -> Color.GREEN;
            };

            g.setColor(carColor);
            g.fillRect(x - 6, y - 4, 12, 8);

            // Draw car label
            g.setColor(Color.WHITE);
            g.drawString("C" + car.id, x - 4, y + 3);
        }

        @Override
        public Dimension getPreferredSize() {
            TrafficMap trafficMap = simulation.getMapManager().getTrafficMap();
            return new Dimension(trafficMap.getWidth() * CELL_SIZE + OFFSET * 2,
                    trafficMap.getHeight() * CELL_SIZE + OFFSET * 2);
        }
    }

    public static boolean isValidNumber(String input) {
        try {
            int value = Integer.parseInt(input.trim()); // Try to parse
            return value > -1 && value < 10000;            // Check range 0–9
        } catch (NumberFormatException e) {
            return false;                               // Not a valid integer
        }
    }

}