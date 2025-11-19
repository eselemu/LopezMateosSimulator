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
        JTextField carNumber = new JTextField("#", 3);

        JLabel semaphoreLabel = new JLabel("Semáforos: ");
        JTextField semaphoreNumber = new JTextField("#", 3);

        JLabel pedestrianLabel = new JLabel("Peatones: ");
        JTextField pedestrianNumber = new JTextField("#", 3);

        JLabel greenTimerLabel = new JLabel("Green Light Timer (s): ");
        JTextField greenTimerInput = new JTextField("#", 3);

        JLabel yellowTimerLabel = new JLabel("Yellow Light Timer (s): ");
        JTextField yellowTimerInput = new JTextField("#", 3);

        JLabel redTimerLabel = new JLabel("Red Light Timer (s): ");
        JTextField redTimerInput = new JTextField("#", 3);

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
            simulation.initializeSimulation(getInputs()[0], getInputs()[1], getInputs()[2], getInputs()[3], getInputs()[4], getInputs()[5]);
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
                case WAITING -> Color.ORANGE;
                case WAITING_SEMAPHORE ->  Color.RED;
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

    public static boolean isValidNumber(String input) {
        try {
            int value = Integer.parseInt(input.trim()); // Try to parse
            return value > -1 && value < 10;            // Check range 0–9
        } catch (NumberFormatException e) {
            return false;                               // Not a valid integer
        }
    }

}