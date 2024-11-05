package com.zfdang.chess.utils;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class CpuInfo {

    // this might not work all the time, so use the below method instead
    public static int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    // https://stackoverflow.com/questions/10133570/availableprocessors-returns-1-for-dualcore-phones
    public static int getCoresCount() {
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(final File pathname) {
                if (Pattern.matches("cpu[0-9]+", pathname.getName()))
                    return true;
                return false;
            }
        }
        try {
            final File dir = new File("/sys/devices/system/cpu/");
            final File[] files = dir.listFiles(new CpuFilter());
            return files.length;
        } catch (final Exception e) {
            return Math.max(1, Runtime.getRuntime().availableProcessors());
        }
    }

    public static void getCpuInfo() {
        // Get the number of available processors
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        Log.d("CpuInfo", "Available processors (cores): " + availableProcessors);

        // Get CPU ABI (Application Binary Interface)
        String[] supportedAbis = Build.SUPPORTED_ABIS;
        Log.d("CpuInfo", "Supported ABIs: ");
        for (String abi : supportedAbis) {
            Log.d("CpuInfo", abi);
        }

        // Get CPU architecture
        String cpuAbi = Build.CPU_ABI;
        Log.d("CpuInfo", "CPU ABI: " + cpuAbi);

        // Get CPU architecture (secondary)
        String cpuAbi2 = Build.CPU_ABI2;
        Log.d("CpuInfo", "CPU ABI2: " + cpuAbi2);

        // Get hardware information
        String hardware = Build.HARDWARE;
        Log.d("CpuInfo", "Hardware: " + hardware);

        // Get device model
        String model = Build.MODEL;
        Log.d("CpuInfo", "Device model: " + model);

        // Get device manufacturer
        String manufacturer = Build.MANUFACTURER;
        Log.d("CpuInfo", "Manufacturer: " + manufacturer);
    }
}