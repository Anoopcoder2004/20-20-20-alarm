import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Alarm App',
      debugShowCheckedModeBanner: false,
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  TimeOfDay? startTime;
  TimeOfDay? endTime;

  DateTime? nextAlarmTime;
  Timer? timer;
  String countdown = "";

  int interval = 20; // ✅ USER INPUT INTERVAL
  final TextEditingController intervalController = TextEditingController(
    text: "20",
  ); // ✅ DEFAULT VALUE

  static const platform = MethodChannel('alarm_channel');

  // 🔥 PICK TIME
  Future<void> pickTime(bool isStart) async {
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.now(),
    );

    if (time != null) {
      setState(() {
        isStart ? startTime = time : endTime = time;
      });
    }
  }

  Future<void> stopAlarms() async {
    try {
      await platform.invokeMethod("stopAlarms");

      timer?.cancel(); // stop countdown
      setState(() {
        countdown = "";
        nextAlarmTime = null;
      });

      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("Alarms Stopped ❌")));
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("Error stopping alarms")));
    }
  }

  // 🔥 CONVERT TIME
  DateTime convertToDateTime(TimeOfDay time) {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day, time.hour, time.minute);
  }

  int convertToMillis(TimeOfDay time) {
    return convertToDateTime(time).millisecondsSinceEpoch;
  }

  // 🔥 CALCULATE NEXT ALARM
  void calculateNextAlarm() {
    if (startTime == null) return;

    DateTime now = DateTime.now();
    DateTime next = convertToDateTime(startTime!);

    while (next.isBefore(now)) {
      next = next.add(Duration(minutes: interval)); // ✅ USE USER INTERVAL
    }

    setState(() {
      nextAlarmTime = next;
    });

    startCountdown();
  }

  // 🔥 COUNTDOWN
  void startCountdown() {
    timer?.cancel();

    timer = Timer.periodic(Duration(seconds: 1), (_) {
      if (nextAlarmTime == null) return;

      final diff = nextAlarmTime!.difference(DateTime.now());

      if (diff.isNegative) {
        calculateNextAlarm();
        return;
      }

      setState(() {
        countdown =
            "${diff.inMinutes.remainder(60).toString().padLeft(2, '0')}:"
            "${diff.inSeconds.remainder(60).toString().padLeft(2, '0')}";
      });
    });
  }

  // 🔥 START ALARMS
  Future<void> startAlarms() async {
    if (startTime == null || endTime == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("Select both times")));
      return;
    }

    if (convertToMillis(startTime!) >= convertToMillis(endTime!)) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("End must be after start")));
      return;
    }

    if (interval <= 0) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("Enter valid interval")));
      return;
    }

    try {
      await platform.invokeMethod("scheduleAlarms", {
        "start": convertToMillis(startTime!),
        "end": convertToMillis(endTime!),
        "interval": interval, // ✅ USER INTERVAL
      });

      calculateNextAlarm();

      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("Alarms Scheduled ✅")));
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("Error: $e")));
    }
  }

  @override
  void dispose() {
    timer?.cancel();
    intervalController.dispose(); // ✅ CLEANUP
    super.dispose();
  }

  // 🔥 UI
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("20-20-20 Alarm")),

      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // TIMES
            if (startTime != null) Text("Start: ${startTime!.format(context)}"),

            if (endTime != null) Text("End: ${endTime!.format(context)}"),

            SizedBox(height: 20),

            // 🔥 INTERVAL INPUT
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 40),
              child: TextField(
                controller: intervalController,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  labelText: "Interval (minutes)",
                  border: OutlineInputBorder(),
                ),
                onChanged: (value) {
                  interval = int.tryParse(value) ?? 1;
                },
              ),
            ),

            SizedBox(height: 20),

            // BUTTONS
            buildButton("Select Start Time", () => pickTime(true)),
            SizedBox(height: 10),
            buildButton("Select End Time", () => pickTime(false)),

            SizedBox(height: 20),

            buildButton("Start Alarm", startAlarms),

            SizedBox(height: 30),
            SizedBox(height: 10),

            buildButton("Stop Alarm", stopAlarms), // ✅ ADDED
            // NEXT ALARM
            if (nextAlarmTime != null)
              Text(
                "Next Alarm: ${nextAlarmTime!.hour}:${nextAlarmTime!.minute.toString().padLeft(2, '0')}",
              ),

            // COUNTDOWN
            if (countdown.isNotEmpty)
              Text(
                "Countdown: $countdown",
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
          ],
        ),
      ),
    );
  }

  // 🔥 REUSABLE BUTTON
  Widget buildButton(String text, VoidCallback onPressed) {
    return SizedBox(
      width: 220,
      child: ElevatedButton(onPressed: onPressed, child: Text(text)),
    );
  }
}
