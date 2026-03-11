package com.eimemes.chat.network;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class StreamingClient {

    private static final String API_URL = "https://eimemeschat-ai-ashy.vercel.app/api/chat";

    public interface Callback {
        void onToken(String token);
        void onDone(String full, String model, boolean disclaimer);
        void onError(String error);
    }

    private final OkHttpClient client;
    private       Call         activeCall;
    private final Handler      main = new Handler(Looper.getMainLooper());

    public StreamingClient() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }

    public void send(String message, JSONArray history, String idToken, Callback cb) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("message", message);
                body.put("history", history != null ? history : new JSONArray());

                Request req = new Request.Builder()
                    .url(API_URL)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + idToken)
                    .build();

                activeCall = client.newCall(req);
                Response resp = activeCall.execute();

                if (!resp.isSuccessful()) {
                    resp.close();
                    main.post(() -> cb.onError("Server error " + resp.code()));
                    return;
                }

                ResponseBody responseBody = resp.body();
                if (responseBody == null) {
                    main.post(() -> cb.onError("Empty response from server."));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseBody.byteStream()));
                StringBuilder full  = new StringBuilder();
                String        line;
                String        model = "";
                boolean       disc  = false;

                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if (data.isEmpty()) continue;
                    try {
                        JSONObject p = new JSONObject(data);
                        if (p.has("token")) {
                            String t = p.getString("token");
                            full.append(t);
                            main.post(() -> cb.onToken(t));
                        } else if (p.optBoolean("done", false)) {
                            model = p.optString("model", "");
                            disc  = p.optBoolean("disclaimer", false);
                        } else if (p.has("error")) {
                            String err = p.getString("error");
                            main.post(() -> cb.onError(err));
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                String  fm = model;
                boolean fd = disc;
                String  ft = full.toString();
                responseBody.close();
                main.post(() -> cb.onDone(ft, fm, fd));

            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("canceled")) return;
                main.post(() -> cb.onError("Connection error. Check your internet."));
            }
        }).start();
    }

    public void cancel() { if (activeCall != null) activeCall.cancel(); }
}

