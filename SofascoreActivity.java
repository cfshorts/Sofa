package com.test.example;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pro.falconadmin.adapters.AddEventsAdapter;
import com.pro.falconadmin.databinding.ActivitySofascoreBinding;
import com.pro.falconadmin.dialogs.AddEventDialog;
import com.pro.falconadmin.models.EventModel;
import com.pro.falconadmin.utils.DataPreferences;
import com.pro.falconadmin.utils.Server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import okhttp3.Request;

public class SofascoreActivity extends AppCompatActivity {
  private static final int CRICKET_ODI_DURATION = 480;
  private static final Map<String, Integer> SPORT_DURATION =
      Map.ofEntries(
          Map.entry("american football", 180),
          Map.entry("badminton", 90),
          Map.entry("baseball", 180),
          Map.entry("basketball", 150),
          Map.entry("cricket", 240),
          Map.entry("e-sports", 150),
          Map.entry("football", 140),
          Map.entry("handball", 90),
          Map.entry("hokey", 90),
          Map.entry("ice hockey", 150),
          Map.entry("mixed martial arts", 60),
          Map.entry("motorsports", 120),
          Map.entry("rugby", 100),
          Map.entry("table tennis", 60),
          Map.entry("tennis", 180),
          Map.entry("volleyball", 120),
          Map.entry("wwe", 180),
          Map.entry("wrestling", 90));

  private final String BASE_URL =
      "https://www.sofascore.com/api/v1/sport/football/scheduled-tournaments/";
  private final String BASE_URL_ENDPOINT = "/page/1";
  private final String EVENTS_URL = "https://www.sofascore.com/api/v1/unique-tournament/";
  private final String TOURNAMENT_IMAGE_URL = "http://api.sofascore.com/api/v1/unique-tournament/";
  private final String TOURNAMENT_IMAGE_URL2 = "/image/dark";
  private final String TEAM_IMAGE_URL = "https://img.sofascore.com/api/v1/team/";
  private final String TEAM_IMAGE_URL2 = "/image";
  private ActivitySofascoreBinding binding;
  private AddEventsAdapter adapter;
  private List<EventModel> models;

  private String dateStr;

  private final Set<String> apies = new HashSet<>();
  private final Set<Integer> eventIds = new HashSet<>();

  @SuppressLint("SetTextI18n")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    super.onCreate(savedInstanceState);
    binding = ActivitySofascoreBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    binding.apiTxt.setText(BASE_URL + getDate() + BASE_URL_ENDPOINT);
    binding.fetchBtn.setOnClickListener(v -> fetchData());
    binding.addBtn.setOnClickListener(v -> addEvents());

    fetchData();

    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            getResources().getStringArray(R.array.sofascore_cats));
    binding.sofaCat.setAdapter(adapter);
    binding.sofaCat.setOnClickListener(v -> binding.sofaCat.showDropDown());
    binding.sofaCat.setOnItemClickListener(
        (parent, view, position, id) ->
            binding.apiTxt.setText(
                BASE_URL.replaceAll("football", adapter.getItem(position).toLowerCase())
                    + getDate()
                    + BASE_URL_ENDPOINT));
  }

  private String getDate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      LocalDate today = LocalDate.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      return today.format(formatter);
    } else {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
      return sdf.format(new Date());
    }
  }

  private void addEvents() {
    if (adapter == null) {
      Toast.makeText(this, "Data not loaded!", Toast.LENGTH_SHORT).show();
      return;
    }

    
  }

  private void fetchData() {
    models = null;
    apies.clear();
    eventIds.clear();
    binding.progress.setVisibility(View.VISIBLE);
    binding.recyclerView.setVisibility(View.GONE);
    try {
      String api = binding.apiTxt.getText().toString();
      dateStr = getDateStr(api);

      Server.sendRequest(getRequest(api), this::parseTournaments);
    } catch (Exception e) {
      Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  private String getDateStr(String api) {
    try {
      URI uri = new URI(api);
      String[] parts = uri.getPath().split("/");

      String date = null;
      for (String part : parts) {
        if (part.matches("\\d{4}-\\d{2}-\\d{2}")) {
          date = part;
          break;
        }
      }

      return date;
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private void parseTournaments(boolean success, String data) {
    try {
      JSONObject dataObj = new JSONObject(data);
      JSONArray array = dataObj.getJSONArray("scheduled");
      for (int i = 0; i < array.length(); ++i) {

        try {
          JSONObject obj = array.getJSONObject(i);
          JSONObject tournament = obj.getJSONObject("tournament");

          JSONObject uT = tournament.getJSONObject("uniqueTournament");
          fetchEvents(uT.getInt("id"));
        } catch (JSONException ignored) {
        }
      }
    } catch (JSONException err) {
      Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  private void fetchEvents(int id) {
    final String api = EVENTS_URL + id + "/scheduled-events/" + dateStr;
    if (!apies.add(api)) {
      return;
    }

    Server.sendRequest(
        getRequest(api),
        (success, data) -> {
          apies.remove(api);

          if (success) {
            setData(data);
          }
        });
  }

  private Request getRequest(String api) {
    return new Request.Builder()
        .url(api)
        .get()
        .header("Cache-Control", "max-age=0")
        .header("X-Requested-With", "6e3f60")
        .header("Sec-CH-UA-Platform", "\"Android\"")
        .header(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 10; TECNO KE5 Build/QP1A.190711.020) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.91 Mobile Safari/537.36")
        .header(
            "Sec-CH-UA",
            "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"")
        .header("Sec-CH-UA-Mobile", "?1")
        .header("Accept", "*/*")
        .header("Sec-Fetch-Site", "same-origin")
        .header("Sec-Fetch-Mode", "cors")
        .header("Sec-Fetch-Dest", "empty")
        .header("Referer", "https://www.sofascore.com/tennis")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Priority", "u=1, i")
        .header(
            "Cookie",
            "_adv_sid=48d95dca-10c2-4c98-b89b-8ff0ba86427a; "
                + "_adv_uid=5d582ce9-4555-407f-9258-ac7cc8c07c1f; "
                + "AD_VERGE_SESSION_COOKIE_V1=11c2f3e4-be27-4d7d-a80c-8d123068131b; "
                + "ssp_test=control; "
                + "_li_dcdm_c=.sofascore.com; "
                + "_lc2_fpi=a78faec1e09d--01kvzz7zhqh4b4yzdxtntdz3mn; "
                + "_lc2_fpi_meta=%7B%22w%22%3A1782410640953%7D; "
                + "panoramaId_expiry=1783015443378; "
                + "_cc_id=c183d69532e217ca06984fa9c498f1a4; "
                + "panoramaId=e951be8b9561e70e9a4cb699b5bd4945a702eb673fe9fb763a26f03edb78849d")
        .build();
  }

  private void setData(String data) {
    if (models == null) {
      models = getModels(data);
    } else {
      models.addAll(getModels(data));
    }

    if (apies.isEmpty()) {
      Collections.sort(
          models,
          (o1, o2) -> {
            if (o1.parseDateTime() == null || o2.parseDateTime() == null) return 0;
            return o1.parseDateTime().compareTo(o2.parseDateTime());
          });

      adapter = new AddEventsAdapter(this, models);
      binding.recyclerView.setAdapter(adapter);
      binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
      binding.recyclerView.setVisibility(View.VISIBLE);
      binding.progress.setVisibility(View.GONE);
    }
  }

  private List<EventModel> getModels(final String data) {
    List<EventModel> models = new ArrayList<>();
    try {
      JSONObject dataObj = new JSONObject(data);
      JSONArray array = dataObj.getJSONArray("events");
      for (int i = 0; i < array.length(); ++i) {

        JSONObject obj = array.getJSONObject(i);
        int eventId = obj.getInt("id");
        if (!eventIds.add(eventId)) {
          continue; // already added
        }
        EventModel model = new EventModel();
        JSONObject tournament = obj.getJSONObject("tournament");

        JSONObject uT = tournament.getJSONObject("uniqueTournament");
        model.setEventName(uT.getString("name"));
        model.setEventLogo(TOURNAMENT_IMAGE_URL + uT.getInt("id") + TOURNAMENT_IMAGE_URL2);
        model.setCategory(uT.getJSONObject("category").getJSONObject("sport").getString("name"));

        JSONObject homeTeam = obj.getJSONObject("homeTeam");
        model.setTeamAName(homeTeam.getString("name"));
        model.setTeamAFlag(TEAM_IMAGE_URL + homeTeam.getInt("id") + TEAM_IMAGE_URL2);

        JSONObject awayTeam = obj.getJSONObject("awayTeam");
        model.setTeamBName(awayTeam.getString("name"));
        model.setTeamBFlag(TEAM_IMAGE_URL + awayTeam.getInt("id") + TEAM_IMAGE_URL2);

        long startTimestamp = obj.getLong("startTimestamp");
        String[] dateTime = parseDateTime(startTimestamp);
        model.setDate(dateTime[0]);
        model.setTime(dateTime[1]);

        setEndDateTime(model, startTimestamp);
        models.add(model);
      }
    } catch (JSONException err) {
      Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
    }

    Collections.sort(
        models,
        (o1, o2) -> {
          if (o1.parseDateTime() == null || o2.parseDateTime() == null) return 0;
          return o1.parseDateTime().compareTo(o2.parseDateTime());
        });

    return models;
  }

  private void setEndDateTime(EventModel model, long startTimestamp) {
    String cat = model.getCategory();
    if (cat == null) {
      return;
    }

    Integer durationMin = SPORT_DURATION.get(cat.toLowerCase());
    if (model.getCategory().equalsIgnoreCase("Cricket")
        && model.getEventName().toLowerCase().contains("odi")) {
      durationMin = CRICKET_ODI_DURATION;
    }

    if (durationMin == null || durationMin <= 0) {
      return;
    }

    long endTimestamp = startTimestamp + (durationMin * 60L);
    String[] endDateTime = parseDateTime(endTimestamp);

    model.setEndDate(endDateTime[0]);
    model.setEndTime(endDateTime[1]);
  }

  private String[] parseDateTime(long timestamp) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Instant instant = Instant.ofEpochSecond(timestamp);
      ZonedDateTime dateTime = instant.atZone(ZoneId.of("GMT"));

      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

      String date = dateTime.format(dateFormatter);
      String time = dateTime.format(timeFormatter);

      return new String[] {date, time};
    } else {
      long millis = timestamp * 1000L;

      SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
      SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

      TimeZone gmt = TimeZone.getTimeZone("GMT");
      dateFormat.setTimeZone(gmt);
      timeFormat.setTimeZone(gmt);

      Date date = new Date(millis);

      return new String[] {dateFormat.format(date), timeFormat.format(date)};
    }
  }
}
