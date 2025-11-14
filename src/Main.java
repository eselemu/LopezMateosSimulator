import simulation.TrafficSimulationCore;
import simulation.agents.Car;
import simulation.ui.TrafficSimulationUI;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
    TrafficSimulationCore simulation = TrafficSimulationCore.getInstance();
    simulation.initializeSimulation();
    simulation.startSimulation();

    // UI
    javax.swing.SwingUtilities.invokeLater(() -> {
        TrafficSimulationUI ui = new TrafficSimulationUI(simulation);
        ui.setVisible(true);
    });
}
