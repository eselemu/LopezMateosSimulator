import simulation.TrafficSimulationCore;
import simulation.agents.Car;
import simulation.ui.AgentVisualizer;
import simulation.ui.ThreadVisualizer;
import simulation.ui.TrafficSimulationUI;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
    TrafficSimulationCore simulation = TrafficSimulationCore.getInstance();
    //simulation.initializeSimulation();
    //simulation.startSimulation();

    // UI
    javax.swing.SwingUtilities.invokeLater(() -> {
        TrafficSimulationUI ui = new TrafficSimulationUI(simulation);
        ui.setVisible(true);
    });

    ThreadVisualizer threadVisualizer = new ThreadVisualizer();
    Thread threadVisualizerThread = new Thread(threadVisualizer);
    threadVisualizerThread.start();

    AgentVisualizer agentVisualizer = new AgentVisualizer();
    Thread agentVisualizerThread = new Thread(agentVisualizer);
    agentVisualizerThread.start();
}
