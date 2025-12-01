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
        private static final int CELL_SIZE = 60; // Larger cells to accommodate sidewalks
        private static final int OFFSET = 50;
        private static final int SIDEWALK_WIDTH = 8; // Width of sidewalk on each side
        private static final int ROAD_WIDTH = CELL_SIZE - (SIDEWALK_WIDTH * 2);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawMap(g);
            drawAgents(g);
        }

        private void drawMap(Graphics g) {
            TrafficMap trafficMap = simulation.getMapManager().getTrafficMap();
            int scale = trafficMap.getScale();

            // Draw background (green for grass/land)
            g.setColor(new Color(34, 139, 34)); // Forest green
            g.fillRect(0, 0, getWidth(), getHeight());

            // Draw the grid with streets and sidewalks
            drawStreetGrid(g, trafficMap);

            // Draw directed edges (arrows) on the roads
            drawRoadArrows(g, trafficMap);
        }

        private void drawStreetGrid(Graphics g, TrafficMap trafficMap) {
            for (int gridX = 0; gridX < trafficMap.getWidth(); gridX++) {
                for (int gridY = 0; gridY < trafficMap.getHeight(); gridY++) {
                    int screenX = OFFSET + gridX * CELL_SIZE;
                    int screenY = OFFSET + gridY * CELL_SIZE;

                    TrafficNode.NodeType nodeType = determineVisualNodeType(gridX, gridY);

                    switch (nodeType) {
                        case INTERSECTION:
                            drawIntersection(g, screenX, screenY);
                            break;
                        case STREET:
                            drawStreetSegment(g, screenX, screenY, gridX, gridY);
                            break;
                        case CROSSWALK:
                            drawCrosswalk(g, screenX, screenY, gridX, gridY);
                            break;
                    }
                }
            }
        }

        private TrafficNode.NodeType determineVisualNodeType(int gridX, int gridY) {
            // This matches the TrafficMap's node type determination
            if (gridX % 2 == 0 && gridY % 2 == 0) {
                return TrafficNode.NodeType.INTERSECTION;
            } else if (gridX % 2 == 1 && gridY % 2 == 0) {
                return TrafficNode.NodeType.STREET; // Horizontal street
            } else if (gridX % 2 == 0 && gridY % 2 == 1) {
                return TrafficNode.NodeType.STREET; // Vertical street
            } else {
                return TrafficNode.NodeType.CROSSWALK;
            }
        }

        private void drawIntersection(Graphics g, int screenX, int screenY) {
            // Draw intersection (dark asphalt)
            g.setColor(new Color(64, 64, 64)); // Dark gray for intersection
            g.fillRect(screenX + SIDEWALK_WIDTH, screenY + SIDEWALK_WIDTH,
                    ROAD_WIDTH, ROAD_WIDTH);

            // Draw sidewalk around intersection
            g.setColor(new Color(184, 134, 11)); // Dark goldenrod for sidewalk
            g.fillRect(screenX, screenY, CELL_SIZE, SIDEWALK_WIDTH); // Top sidewalk
            g.fillRect(screenX, screenY + CELL_SIZE - SIDEWALK_WIDTH, CELL_SIZE, SIDEWALK_WIDTH); // Bottom sidewalk
            g.fillRect(screenX, screenY, SIDEWALK_WIDTH, CELL_SIZE); // Left sidewalk
            g.fillRect(screenX + CELL_SIZE - SIDEWALK_WIDTH, screenY, SIDEWALK_WIDTH, CELL_SIZE); // Right sidewalk

            // Draw intersection markings (white lines)
            g.setColor(Color.WHITE);
            g.drawRect(screenX + SIDEWALK_WIDTH, screenY + SIDEWALK_WIDTH,
                    ROAD_WIDTH - 1, ROAD_WIDTH - 1);
        }

        private void drawStreetSegment(Graphics g, int screenX, int screenY, int gridX, int gridY) {
            if (gridX % 2 == 1) { // Horizontal street
                drawHorizontalStreet(g, screenX, screenY);
            } else { // Vertical street
                drawVerticalStreet(g, screenX, screenY);
            }
        }

        private void drawHorizontalStreet(Graphics g, int screenX, int screenY) {
            // Draw road (dark asphalt)
            g.setColor(new Color(64, 64, 64));
            g.fillRect(screenX, screenY + SIDEWALK_WIDTH, CELL_SIZE, ROAD_WIDTH);

            // Draw sidewalks (top and bottom)
            g.setColor(new Color(184, 134, 11));
            g.fillRect(screenX, screenY, CELL_SIZE, SIDEWALK_WIDTH); // Top sidewalk
            g.fillRect(screenX, screenY + CELL_SIZE - SIDEWALK_WIDTH, CELL_SIZE, SIDEWALK_WIDTH); // Bottom sidewalk

            // Draw lane markings (dashed white lines)
            g.setColor(Color.WHITE);
            int centerY = screenY + CELL_SIZE / 2;
            for (int x = screenX + 5; x < screenX + CELL_SIZE; x += 10) {
                g.fillRect(x, centerY - 1, 5, 2);
            }
        }

        private void drawVerticalStreet(Graphics g, int screenX, int screenY) {
            // Draw road (dark asphalt)
            g.setColor(new Color(64, 64, 64));
            g.fillRect(screenX + SIDEWALK_WIDTH, screenY, ROAD_WIDTH, CELL_SIZE);

            // Draw sidewalks (left and right)
            g.setColor(new Color(184, 134, 11));
            g.fillRect(screenX, screenY, SIDEWALK_WIDTH, CELL_SIZE); // Left sidewalk
            g.fillRect(screenX + CELL_SIZE - SIDEWALK_WIDTH, screenY, SIDEWALK_WIDTH, CELL_SIZE); // Right sidewalk

            // Draw lane markings (dashed white lines)
            g.setColor(Color.WHITE);
            int centerX = screenX + CELL_SIZE / 2;
            for (int y = screenY + 5; y < screenY + CELL_SIZE; y += 10) {
                g.fillRect(centerX - 1, y, 2, 5);
            }
        }

        private void drawCrosswalk(Graphics g, int screenX, int screenY, int gridX, int gridY) {
            // Crosswalks are at diagonal positions between intersections
            g.setColor(new Color(184, 134, 11)); // Sidewalk color

            if (gridX % 2 == 1 && gridY % 2 == 1) {
                // This is a crosswalk position - draw zebra crossing
                g.fillRect(screenX, screenY, CELL_SIZE, CELL_SIZE);

                // Draw zebra stripes
                g.setColor(Color.WHITE);
                if (gridX > gridY) { // Horizontal crosswalk
                    for (int x = screenX + 5; x < screenX + CELL_SIZE; x += 10) {
                        g.fillRect(x, screenY, 5, CELL_SIZE);
                    }
                } else { // Vertical crosswalk
                    for (int y = screenY + 5; y < screenY + CELL_SIZE; y += 10) {
                        g.fillRect(screenX, y, CELL_SIZE, 5);
                    }
                }
            }
        }

        private void drawRoadArrows(Graphics g, TrafficMap trafficMap) {
            g.setColor(Color.YELLOW); // Yellow arrows for better visibility

            for (TrafficEdge edge : trafficMap.getEdges().values()) {
                Position fromPos = edge.getFrom().position;
                Position toPos = edge.getTo().position;

                int fromGridX = fromPos.x / trafficMap.getScale();
                int fromGridY = fromPos.y / trafficMap.getScale();
                int toGridX = toPos.x / trafficMap.getScale();
                int toGridY = toPos.y / trafficMap.getScale();

                int fromX = OFFSET + fromGridX * CELL_SIZE + CELL_SIZE / 2;
                int fromY = OFFSET + fromGridY * CELL_SIZE + CELL_SIZE / 2;
                int toX = OFFSET + toGridX * CELL_SIZE + CELL_SIZE / 2;
                int toY = OFFSET + toGridY * CELL_SIZE + CELL_SIZE / 2;

                // Only draw arrows for the main road segments (not the intermediate ones)
                if (isMainRoadSegment(fromGridX, fromGridY, toGridX, toGridY)) {
                    drawDirectionArrow(g, fromX, fromY, toX, toY);
                }
            }
        }

        private boolean isMainRoadSegment(int fromX, int fromY, int toX, int toY) {
            // Check if this is a segment between two intersections (not including street nodes)
            return (fromX % 2 == 0 && fromY % 2 == 0 && toX % 2 == 0 && toY % 2 == 0);
        }

        private void drawDirectionArrow(Graphics g, int fromX, int fromY, int toX, int toY) {
            // Calculate arrow position (closer to the center of the segment)
            int arrowX = (fromX + toX) / 2;
            int arrowY = (fromY + toY) / 2;

            double angle = Math.atan2(toY - fromY, toX - fromX);
            int arrowSize = 6;

            // Draw arrow line
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(arrowX - (int)(arrowSize * 1.5 * Math.cos(angle)),
                    arrowY - (int)(arrowSize * 1.5 * Math.sin(angle)),
                    arrowX, arrowY);

            // Draw arrow head
            int x3 = (int) (arrowX - arrowSize * Math.cos(angle - Math.PI / 6));
            int y3 = (int) (arrowY - arrowSize * Math.sin(angle - Math.PI / 6));
            int x4 = (int) (arrowX - arrowSize * Math.cos(angle + Math.PI / 6));
            int y4 = (int) (arrowY - arrowSize * Math.sin(angle + Math.PI / 6));

            g2d.drawLine(arrowX, arrowY, x3, y3);
            g2d.drawLine(arrowX, arrowY, x4, y4);
        }

        private void drawAgents(Graphics g) {
            List<Car> cars = simulation.getCars();
            List<Truck> trucks = simulation.getTrucks();
            List<SemaphoreSimulation> semaphores = simulation.getSemaphores();

            TrafficMap trafficMap = simulation.getMapManager().getTrafficMap();
            int scale = trafficMap.getScale();

            // Draw semáforos on sidewalks
            for(SemaphoreSimulation semaphoreSimulation : semaphores) {
                drawSemaphore(g, semaphoreSimulation, trafficMap, scale);
            }

            // Draw trucks on roads
            for(Truck truck : trucks) {
                drawTruck(g, truck, trafficMap, scale);
            }

            // Draw cars on roads
            for(Car car : cars) {
                drawCar(g, car, trafficMap, scale);
            }
        }

        private void drawSemaphore(Graphics g, SemaphoreSimulation semaphore, TrafficMap trafficMap, int scale) {
            Position pos = semaphore.getPosition();
            int gridX = pos.x / scale;
            int gridY = pos.y / scale;

            int screenX = OFFSET + gridX * CELL_SIZE;
            int screenY = OFFSET + gridY * CELL_SIZE;

            // Place semaphore on the sidewalk (top-right corner of intersection)
            int semaphoreX = screenX + CELL_SIZE - SIDEWALK_WIDTH + 2;
            int semaphoreY = screenY + 5;

            // Draw semaphore pole (on sidewalk)
            g.setColor(Color.BLACK);
            g.fillRect(semaphoreX, semaphoreY, 3, 15);

            // Draw semaphore box
            g.setColor(Color.DARK_GRAY);
            g.fillRect(semaphoreX - 3, semaphoreY - 10, 9, 8);

            // Draw current light
            Color lightColor = switch(semaphore.getCurrentState()) {
                case RED -> Color.RED;
                case YELLOW -> Color.YELLOW;
                case GREEN -> Color.GREEN;
            };
            g.setColor(lightColor);
            g.fillOval(semaphoreX - 2, semaphoreY - 8, 5, 5);

            // Draw semaphore ID label
            g.setColor(Color.BLACK);
            g.drawString("S" + semaphore.id, semaphoreX - 5, semaphoreY + 25);
        }

        private void drawTruck(Graphics g, Truck truck, TrafficMap trafficMap, int scale) {
            Position frontPos = truck.getCurrentPosition();
            Position rearPos = truck.getRearPosition();

            int frontX = OFFSET + (frontPos.x / scale) * CELL_SIZE + CELL_SIZE / 2;
            int frontY = OFFSET + (frontPos.y / scale) * CELL_SIZE + CELL_SIZE / 2;
            int rearX = OFFSET + (rearPos.x / scale) * CELL_SIZE + CELL_SIZE / 2;
            int rearY = OFFSET + (rearPos.y / scale) * CELL_SIZE + CELL_SIZE / 2;

            // Color según estado
            Color truckColor = switch(truck.getTruckState()) {
                case MOVING -> new Color(139, 69, 19); // Brown
                case WAITING -> Color.ORANGE;
                case WAITING_SEMAPHORE -> Color.RED;
                case FINISHED -> Color.GREEN;
            };

            g.setColor(truckColor);

            // Calculate truck position and orientation
            int truckCenterX = (frontX + rearX) / 2;
            int truckCenterY = (frontY + rearY) / 2;
            int truckLength = (int) Math.sqrt(Math.pow(frontX - rearX, 2) + Math.pow(frontY - rearY, 2));
            double angle = Math.atan2(frontY - rearY, frontX - rearX);

            // Draw truck as a rotated rectangle
            Graphics2D g2d = (Graphics2D) g;
            g2d.rotate(angle, truckCenterX, truckCenterY);

            int truckWidth = 12;
            g2d.fillRect(truckCenterX - truckLength/2, truckCenterY - truckWidth/2,
                    truckLength, truckWidth);

            // Draw cab (different color)
            g2d.setColor(new Color(70, 130, 180)); // Steel blue
            g2d.fillRect(truckCenterX + truckLength/2 - 8, truckCenterY - truckWidth/2, 8, truckWidth);

            g2d.rotate(-angle, truckCenterX, truckCenterY); // Reset rotation

            // Draw truck label
            g.setColor(Color.WHITE);
            g.drawString("T" + truck.id, truckCenterX - 5, truckCenterY + 15);
        }

        private void drawCar(Graphics g, Car car, TrafficMap trafficMap, int scale) {
            Position pos = car.getCurrentPosition();
            int gridX = pos.x / scale;
            int gridY = pos.y / scale;

            int screenX = OFFSET + gridX * CELL_SIZE + CELL_SIZE / 2;
            int screenY = OFFSET + gridY * CELL_SIZE + CELL_SIZE / 2;

            // Color según estado
            Color carColor = switch(car.getCarState()) {
                case MOVING -> Color.BLUE;
                case WAITING -> Color.ORANGE;
                case WAITING_SEMAPHORE -> Color.RED;
                case FINISHED -> Color.GREEN;
            };

            g.setColor(carColor);

            // Draw car body
            g.fillRect(screenX - 8, screenY - 5, 16, 10);

            // Draw car windows
            g.setColor(Color.CYAN);
            g.fillRect(screenX - 6, screenY - 3, 5, 4);
            g.fillRect(screenX + 1, screenY - 3, 5, 4);

            // Draw wheels
            g.setColor(Color.BLACK);
            g.fillOval(screenX - 7, screenY + 3, 4, 4);
            g.fillOval(screenX + 3, screenY + 3, 4, 4);

            // Draw car label
            g.setColor(Color.WHITE);
            g.drawString("C" + car.id, screenX - 4, screenY - 8);
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