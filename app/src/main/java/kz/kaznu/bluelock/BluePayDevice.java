package kz.kaznu.bluelock;

import android.bluetooth.BluetoothDevice;

/**
 * Created by c0rp on 1/29/17.
 */

public class BluePayDevice implements Comparable {

    private String name;
    private String uniqueCode;
    private String MACaddress;
    private Integer rssi;
    private BluetoothDevice device;

    public BluePayDevice(String name, String uniqueCode, String MACaddress, Integer rssi,
                         BluetoothDevice device) {
        this.name = name;
        this.uniqueCode = uniqueCode;
        this.MACaddress = MACaddress;
        this.rssi = rssi;
        this.device = device;
    }

    @Override
    public int compareTo(Object o) {

        if (!(o instanceof BluePayDevice)) {
            throw new RuntimeException();
        }

        return ((BluePayDevice) o).getRssi().compareTo(rssi);
    }

    @Override
    public String toString() {
        return name + "_" + uniqueCode + "_" + rssi;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public void setUniqueCode(String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getMACaddress() {
        return MACaddress;
    }

    public void setMACaddress(String MACaddress) {
        this.MACaddress = MACaddress;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }
}
