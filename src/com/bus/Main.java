package com.bus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Prosimo, vnesite naslednje argumente: id postaje, stevilo naslednjih avtobusov, " +
                    "casovni format (relative|absolute)");
            return;
        }

        // argumenti
        String stopId = args[0];
        String maxNumOfBussesStr = args[1];
        String timeFormat = args[2];

        // preveri št. avtobusov
        int maxNumOfBusses;
        try {
            maxNumOfBusses = Integer.parseInt(maxNumOfBussesStr);
        } catch (NumberFormatException e) {
            System.out.println("Prosimo, vnesite veljavno celo stevilo za stevilo naslednjih avtobusov.");
            return;
        }

        // preveri format
        final String rel = "relative";
        if (!timeFormat.equals(rel) && !timeFormat.equals("absolute")) {
            System.out.println("Prosimo, vnesite veljaven casovni format: 'relative' ali 'absolute'.");
            return;
        }

        // najdi ime postaje
        String stopName = FindNameById(stopId);

        // preveri ce id obstaja
        if(stopName.isEmpty()){
            System.out.println("Vnesen id postaje ne obstaja.");
            return;
        }

        // zapisi razpored vseh avtobusov
        Map<String, List<LocalTime>> scheduleMap = ReadStopTimes(stopId);

        // sortiraj in filtriraj
        Map<String, List<LocalTime>> filteredScheduleMap = FilterTimeList(scheduleMap, maxNumOfBusses);

        // Izpis
        System.out.println(stopName);
        //System.out.println("Stevilo naslednjih avtobusov: " + maxNumOfBusses);

        for (Map.Entry<String, List<LocalTime>> entry : filteredScheduleMap.entrySet()) {
            String busLine = entry.getKey();
            List<String> timeList = entry.getValue().stream().map(LocalTime::toString).collect(Collectors.toList());

            System.out.print(busLine + ": ");
            if (timeFormat.equals(rel)) {
                for (int i = 0; i < timeList.size(); i++) {
                    LocalTime formatTime = LocalTime.parse(timeList.get(i));
                    timeList.set(i, formatTime.getHour() * 60 + formatTime.getMinute() + "min");
                }
            }
            System.out.println(String.join(", ", timeList));
            //System.out.println("Bus Line " + busLine + ": " + timeList);
        }
        if (filteredScheduleMap.isEmpty()){
            System.out.println("V naslednjih dveh urah ni avtobusov.");
        }
    }

    static String FindNameById(String stopId) {
        String stopName = "";
        try (BufferedReader br = new BufferedReader(new FileReader("gtfs/stops.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(stopId)) {
                    stopName = parts[2];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopName;
    }

    static Map<String, List<LocalTime>> ReadStopTimes(String stopId) {
        Map<String, List<LocalTime>> scheduleMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("gtfs/stop_times.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[3].equals(stopId)) {
                    String linija = parts[0].split("_")[2];
                    if (!scheduleMap.containsKey(linija)) {
                        scheduleMap.put(linija, new ArrayList<>());
                    }
                    scheduleMap.get(linija).add(LocalTime.parse(parts[1]).withSecond(0));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scheduleMap;
    }

    public static Map<String, List<LocalTime>> FilterTimeList(Map<String, List<LocalTime>> scheduleMap, int maxNumOfBusses) {
        LocalTime maxTime = LocalTime.now().plusHours(2);
        //System.out.println("max " + maxTime + "local " + LocalTime.now());
        Map<String, List<LocalTime>> filteredScheduleMap = new HashMap<>();
        for (Map.Entry<String, List<LocalTime>> entry : scheduleMap.entrySet()) {
            String busLine = entry.getKey();
            List<LocalTime> timeList = entry.getValue();
            Collections.sort(timeList);
            filteredScheduleMap.put(busLine, new ArrayList<>());

            // filtriramo za najvec naslednji 2 uri od zahteve
            for (LocalTime checkTime : timeList) {
                if (checkTime.compareTo(maxTime) < 0) {
                    if (filteredScheduleMap.get(busLine).size() < maxNumOfBusses) {
                        filteredScheduleMap.get(busLine).add(checkTime);
                    }
                }
            }
            if (filteredScheduleMap.get(busLine).isEmpty()) {
                filteredScheduleMap.remove(busLine);
            }
        }
        return filteredScheduleMap;
    }
}