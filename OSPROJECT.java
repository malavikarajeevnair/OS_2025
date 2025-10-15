import javax.swing.*;
import java.awt.*;
import java.util.*;

class Process {
    int pid;
    int burstTime;
    int remainingTime;
    int completionTime;
    int waitingTime;
    int turnaroundTime;

    Process(int pid, int burstTime) {
        this.pid = pid;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
    }
}

public class ProcessSchedulingSwing extends JFrame {

    private DefaultListModel<String> processListModel = new DefaultListModel<>();
    private java.util.List<Process> processes = new ArrayList<>();
    private JTextArea outputArea;
    private JButton addProcessBtn, runBtn;
    private JTextField burstTimeField, quantumField;
    private JComboBox<String> algorithmCombo;

    public ProcessSchedulingSwing() {
        setTitle("Multiple Process Scheduling");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ----- Input Panel -----
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("Burst Time:"));
        burstTimeField = new JTextField(5);
        inputPanel.add(burstTimeField);

        addProcessBtn = new JButton("Add Process");
        inputPanel.add(addProcessBtn);

        inputPanel.add(new JLabel("Algorithm:"));
        algorithmCombo = new JComboBox<>(new String[]{"FCFS", "Round Robin"});
        inputPanel.add(algorithmCombo);

        inputPanel.add(new JLabel("Time Quantum (for RR):"));
        quantumField = new JTextField(5);
        quantumField.setEnabled(false);
        inputPanel.add(quantumField);

        runBtn = new JButton("Run Scheduling");
        inputPanel.add(runBtn);

        add(inputPanel, BorderLayout.NORTH);

        // ----- Process List Panel -----
        JPanel processListPanel = new JPanel(new BorderLayout());
        processListPanel.setBorder(BorderFactory.createTitledBorder("Processes (PID : Burst Time)"));

        JList<String> processList = new JList<>(processListModel);
        processListPanel.add(new JScrollPane(processList), BorderLayout.CENTER);
        add(processListPanel, BorderLayout.WEST);

        // ----- Output Area -----
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Scheduling Output"));
        add(outputScroll, BorderLayout.CENTER);

        // ----- Event Handlers -----
        addProcessBtn.addActionListener(e -> addProcess());
        runBtn.addActionListener(e -> runScheduling());
        algorithmCombo.addActionListener(e -> {
            boolean rrSelected = algorithmCombo.getSelectedItem().toString().equals("Round Robin");
            quantumField.setEnabled(rrSelected);
        });
    }

    private void addProcess() {
        try {
            int burst = Integer.parseInt(burstTimeField.getText().trim());
            if (burst <= 0) {
                JOptionPane.showMessageDialog(this, "Burst Time must be a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int pid = processes.size() + 1;
            processes.add(new Process(pid, burst));
            processListModel.addElement("P" + pid + " : " + burst);
            burstTimeField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid integer for burst time.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runScheduling() {
        if (processes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one process first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String alg = algorithmCombo.getSelectedItem().toString();

        // Reset process state
        for (Process p : processes) {
            p.remainingTime = p.burstTime;
            p.completionTime = 0;
            p.waitingTime = 0;
            p.turnaroundTime = 0;
        }

        outputArea.setText("");
        if (alg.equals("FCFS")) {
            fcfsScheduling();
        } else {
            try {
                int quantum = Integer.parseInt(quantumField.getText().trim());
                if (quantum <= 0) {
                    JOptionPane.showMessageDialog(this, "Time Quantum must be positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                roundRobinScheduling(quantum);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid integer for Time Quantum.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ---------- FCFS Scheduling ----------
    private void fcfsScheduling() {
        int currentTime = 0;
        double totalWT = 0, totalTAT = 0;

        outputArea.append("Scheduling using First Come First Serve (FCFS)\n");
        outputArea.append(String.format("%-6s %-10s %-12s %-12s %-10s%n",
                "PID", "Burst", "Completion", "Turnaround", "Waiting"));

        for (Process p : processes) {
            currentTime += p.burstTime;
            p.completionTime = currentTime;
            p.turnaroundTime = p.completionTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;

            totalWT += p.waitingTime;
            totalTAT += p.turnaroundTime;

            outputArea.append(String.format("%-6s %-10d %-12d %-12d %-10d%n",
                    "P" + p.pid, p.burstTime, p.completionTime, p.turnaroundTime, p.waitingTime));
        }

        int n = processes.size();
        outputArea.append(String.format("%nAverage Turnaround Time: %.2f%n", totalTAT / n));
        outputArea.append(String.format("Average Waiting Time: %.2f%n", totalWT / n));
    }

    // ---------- Round Robin Scheduling ----------
    private void roundRobinScheduling(int quantum) {
        int currentTime = 0;
        Queue<Process> queue = new LinkedList<>(processes);
        java.util.List<String> gantt = new ArrayList<>();

        outputArea.append("Scheduling using Round Robin (Quantum = " + quantum + ")\n");
        outputArea.append(String.format("%-6s %-10s %-12s %-12s %-10s%n",
                "PID", "Burst", "Completion", "Turnaround", "Waiting"));

        while (!queue.isEmpty()) {
            Process p = queue.poll();
            int execTime = Math.min(quantum, p.remainingTime);
            int start = currentTime;
            currentTime += execTime;
            p.remainingTime -= execTime;

            gantt.add("P" + p.pid + "[" + start + "-" + currentTime + "]");

            if (p.remainingTime > 0) {
                queue.add(p);
            } else {
                p.completionTime = currentTime;
                p.turnaroundTime = p.completionTime; // arrival=0
                p.waitingTime = p.turnaroundTime - p.burstTime;
            }
        }

        double totalWT = 0, totalTAT = 0;
        for (Process p : processes) {
            outputArea.append(String.format("%-6s %-10d %-12d %-12d %-10d%n",
                    "P" + p.pid, p.burstTime, p.completionTime, p.turnaroundTime, p.waitingTime));
            totalWT += p.waitingTime;
            totalTAT += p.turnaroundTime;
        }

        int n = processes.size();
        outputArea.append(String.format("%nAverage Turnaround Time: %.2f%n", totalTAT / n));
        outputArea.append(String.format("Average Waiting Time: %.2f%n", totalWT / n));

        outputArea.append("\nGantt Chart:\n");
        for (String seg : gantt) outputArea.append(seg + " ");
        outputArea.append("\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProcessSchedulingSwing frame = new ProcessSchedulingSwing();
            frame.setVisible(true);
        });
    }
}