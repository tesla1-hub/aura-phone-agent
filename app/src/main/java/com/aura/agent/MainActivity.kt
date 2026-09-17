package com.aura.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.agent.ui.theme.AuraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale

private data class WeatherDay(val date: String, val high: Double, val low: Double, val rainChance: Int, val code: Int)
private data class Weather(
    val place: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val wind: Double,
    val code: Int,
    val days: List<WeatherDay>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AuraTheme { WeatherDashboard() } }
    }
}

@Composable
private fun WeatherDashboard() {
    var city by remember { mutableStateOf("New York") }
    var weather by remember { mutableStateOf<Weather?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        error = null
        try {
            weather = withContext(Dispatchers.IO) { WeatherApi.fetch(city) }
        } catch (e: Exception) {
            error = e.message ?: "Unable to load weather"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Weather dashboard") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { if (city.isNotBlank()) kotlinx.coroutines.MainScope().launch { refresh() } }) {
                        Text("Search")
                    }
                }
            }
            item {
                when {
                    loading -> CircularProgressIndicator()
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    weather != null -> CurrentWeather(weather!!)
                }
            }
            if (weather != null) {
                item { Text("7-day forecast", style = MaterialTheme.typography.titleLarge) }
                items(weather!!.days) { day -> ForecastCard(day) }
                item {
                    Text("Data provided by Open-Meteo", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CurrentWeather(w: Weather) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(w.place, style = MaterialTheme.typography.headlineSmall)
            Text("${w.temperature.roundToInt()}°  ${description(w.code)}", style = MaterialTheme.typography.displaySmall)
            Text("Feels like ${w.feelsLike.roundToInt()}°")
            Text("Humidity ${w.humidity}%  •  Wind ${w.wind.roundToInt()} km/h")
        }
    }
}

@Composable
private fun ForecastCard(day: WeatherDay) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(day.date); Text(description(day.code), style = MaterialTheme.typography.bodySmall) }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("${day.high.roundToInt()}° / ${day.low.roundToInt()}°")
                Text("Rain ${day.rainChance}%", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private object WeatherApi {
    fun fetch(city: String): Weather {
        val encoded = URLEncoder.encode(city.trim(), "UTF-8")
        val location = getJson("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
            .optJSONArray("results")?.optJSONObject(0) ?: error("City not found")
        val latitude = location.getDouble("latitude")
        val longitude = location.getDouble("longitude")
        val forecast = getJson("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=7")
        val current = forecast.getJSONObject("current")
        val daily = forecast.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val highs = daily.getJSONArray("temperature_2m_max")
        val lows = daily.getJSONArray("temperature_2m_min")
        val rain = daily.getJSONArray("precipitation_probability_max")
        val days = (0 until dates.length()).map { i -> WeatherDay(dates.getString(i), highs.getDouble(i), lows.getDouble(i), rain.getInt(i), codes.getInt(i)) }
        return Weather(location.optString("name", city), current.getDouble("temperature_2m"), current.getDouble("apparent_temperature"), current.getInt("relative_humidity_2m"), current.getDouble("wind_speed_10m"), current.getInt("weather_code"), days)
    }

    private fun getJson(endpoint: String): JSONObject {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        return try {
            if (connection.responseCode !in 200..299) error("Weather service returned ${connection.responseCode}")
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }
}

private fun description(code: Int): String = when (code) {
    0 -> "Clear sky"
    1, 2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Unknown"
}
