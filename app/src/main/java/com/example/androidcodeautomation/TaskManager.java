public class MainActivity extends Activity {
    private TaskManager taskManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize the manager
        taskManager = new TaskManager();
    }

    // Example usage on a button click
    private void onStartClicked() {
        taskManager.startTask();
    }

    private void onStopClicked() {
        taskManager.stopTask();
    }

    @Override
    protected void onDestroy() {
        // Always clean up resources when the Activity dies
        if (taskManager != null) {
            taskManager.stopTask();
        }
        super.onDestroy();
    }
}
