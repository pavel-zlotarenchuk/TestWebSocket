package com.tanat.myapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tanat.myapplication.entity.Operation;
import com.tanat.myapplication.entity.Operations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import ws.wamp.jawampa.CustomWampClient;
import ws.wamp.jawampa.CustomWampClientBuilder;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;

public class MainActivity extends AppCompatActivity {

    private String URL = "ws://url_web_socket";
    private String REALM = "wamp.pusher";

    static final int TIMER_INTERVAL = 1000; // 1s

    private EditText channelEditText;
    private Button connectButton;
    private TextView logTextView;

    private boolean webSoketWork;
    private WebSocketAsyncTask webSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        channelEditText = (EditText) findViewById(R.id.channelEditText);
        connectButton = (Button) findViewById(R.id.connectButton);
        logTextView = (TextView) findViewById(R.id.logTextView);

        channelEditText.setText("channel");
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!webSoketWork) {
                    webSoketWork = true;
                    String channel = String.valueOf(channelEditText.getText());
                    webSocket = new WebSocketAsyncTask();
                    String[] parametrs = {URL, REALM, channel};
                    webSocket.execute(parametrs);
                    connectButton.setText("Disconnect");
                } else {
                    webSoketWork = false;
                    webSocket.cancel(true);
                    connectButton.setText("Connect");
                }
            }
        });
    }

    private class WebSocketAsyncTask extends AsyncTask<String, String, String> {
        private String url;
        private String realm;
        private String channel;

        private CustomWampClient client;
        private Subscription onHelloSubscription;

        // Scheduler for this example
        private ExecutorService executor = Executors.newSingleThreadExecutor();
        private Scheduler rxScheduler = Schedulers.from(executor);

/*        WebSocketAsyncTask(String url, String realm, String channel){
            this.url = url;
            this.realm = realm;
            this.channel = channel;
        }*/

        @Override
        protected String doInBackground(String... parameter) {
             String url = String.valueOf(parameter[0]);
             String realm = String.valueOf(parameter[1]);
             final String channel = String.valueOf(parameter[2]);

            CustomWampClientBuilder builder = new CustomWampClientBuilder();
            IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
            try {

                builder.withConnectorProvider(connectorProvider)
                        .withUri(url)
                        .withRealm(realm)
                        .withInfiniteReconnects()
                        .withCloseOnErrors(true)
                        .withReconnectInterval(5, TimeUnit.SECONDS);
                client = builder.build();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e;
            }

            // Subscribe on the clients status updates
            client.statusChanged()
                    .observeOn(rxScheduler)
                    .subscribe(new Action1<WampClient.State>() {
                        @Override
                        public void call(WampClient.State t1) {
                            System.out.println("Session status changed to " + t1);
                            publishProgress("Session status changed to " + t1);

                            if (t1 instanceof WampClient.ConnectedState) {
                                onHelloSubscription = client.makeSubscription(channel, String.class)
                                        .observeOn(rxScheduler)
                                        .subscribe(new Action1<String>() {
                                            @Override
                                            public void call(String msg) {
                                                Operations operations = new Operations();
                                                operations = operations.parseJson(msg);
                                                for (Operation operation : operations.getOperations()) {
                                                    System.out.println("[" + operation.getType() +
                                                            "] Price: " + operation.getPrice() +
                                                            " | Amount: " + operation.getAmount() +
                                                            " | Time: " + operation.getTime());

                                                    publishProgress("[" + operation.getType() +
                                                            "] Price: " + operation.getPrice() +
                                                            " | Amount: " + operation.getAmount() +
                                                            " | Time: " + operation.getTime());
                                                }
                                            }
                                        }, new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable e) {
                                                publishProgress("Failed to connection: " + e);
                                            }
                                        }, new Action0() {
                                            @Override
                                            public void call() {
                                                publishProgress("Connection ended");
                                            }
                                        });
                            } else if (t1 instanceof WampClient.DisconnectedState) {
                                closeSubscriptions();
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable t) {
                            publishProgress("Session ended with error " + t);
                        }
                    }, new Action0() {
                        @Override
                        public void call() {
                            publishProgress("Session ended normally");
                        }
                    });

            client.open();
            try {
                client.getTerminationFuture().get();
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            logTextView.setText(logTextView.getText() + "\n" + progress[progress.length - 1]);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            logTextView.setText(logTextView.getText() + "\nShutting down");
            closeSubscriptions();
            client.close();
        }

        private void closeSubscriptions() {
            if (onHelloSubscription != null)
                onHelloSubscription.unsubscribe();
            onHelloSubscription = null;
        }
    }
}
