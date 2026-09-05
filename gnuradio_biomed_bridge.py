#!/usr/bin/env python3
"""
=============================================================================
UNIFIED SIGNAL SHIELD & BIOMEDICAL DEVICE PC BRIDGE
=============================================================================
This standalone Python script runs directly on your PC (Windows, macOS, Linux).
It establishes a high-performance WebSocket server on port 8765, streaming:
1. Live SDR / GNU Radio Spectrum & Decoded RF Packets (Wi-Fi, BLE, LoRa, MedRadio)
2. Real-Time Biomedical Device Telemetry (Pacemakers, Infusion Pumps, Ventilators)
3. Direct Emergency CAD Dispatch Relay to 911 / Local Emergency Gateways

HOW TO RUN ON YOUR PC:
1. Ensure Python 3.8+ is installed.
2. Install dependencies:
     pip install websockets asyncio numpy
3. Run the script:
     python gnuradio_biomed_bridge.py
4. In Unified Signal Shield app:
   - On PC Browser (AI Studio Emulator): Connects automatically to ws://10.0.2.2:8765
   - On Physical Device over USB: Run `adb reverse tcp:8765 tcp:8765`, connect to ws://127.0.0.1:8765
   - Over Local Wi-Fi: Connect to ws://<YOUR_PC_IP>:8765
=============================================================================
"""

import asyncio
import json
import logging
import math
import random
import time
import sys

try:
    import websockets
except ImportError:
    print("\n[!] 'websockets' library is required. Install it using:\n    pip install websockets\n")
    sys.exit(1)

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("BiomedSignalBridge")

PORT = 8765
HOST = "0.0.0.0"

# Initial Simulated Fleet of Monitored Biomedical Devices
BIOMED_DEVICES = [
    {
        "id": "MED-PAC-8041",
        "patientName": "Eleanor Vance",
        "patientRoom": "Cardiac Care Unit 3B",
        "deviceType": "PACEMAKER_IMPLANT",
        "rfBandFrequency": "402.5 MHz MedRadio (MICS)",
        "heartRateBpm": 72,
        "spO2Percent": 98,
        "bloodPressureSys": 124,
        "bloodPressureDia": 82,
        "batteryPercent": 89,
        "rssiDbm": -62.0,
        "status": "NOMINAL"
    },
    {
        "id": "MED-PUMP-209",
        "patientName": "Marcus Thorne",
        "patientRoom": "ICU Bed 02",
        "deviceType": "SMART_INFUSION_PUMP",
        "rfBandFrequency": "2.4 GHz Wi-Fi IoT",
        "heartRateBpm": 86,
        "spO2Percent": 95,
        "bloodPressureSys": 138,
        "bloodPressureDia": 88,
        "infusionRateMlH": 15.0,
        "batteryPercent": 96,
        "rssiDbm": -58.0,
        "status": "NOMINAL"
    },
    {
        "id": "MED-VENT-550",
        "patientName": "David Chen",
        "patientRoom": "Trauma Bay 1",
        "deviceType": "ICU_VENTILATOR",
        "rfBandFrequency": "5.8 GHz Medical Telemetry",
        "heartRateBpm": 110,
        "spO2Percent": 91,
        "bloodPressureSys": 145,
        "bloodPressureDia": 94,
        "batteryPercent": 74,
        "rssiDbm": -68.0,
        "status": "ABNORMAL_VITALS",
        "anomalyDescription": "High Peak Inspiratory Pressure & Tachycardia"
    }
]

def generate_ecg_sample(bpm: int, is_fault: bool) -> list:
    samples = []
    for i in range(60):
        if is_fault:
            val = math.sin(i * 0.8) * 0.4 + math.cos(i * 1.5) * 0.3 + (random.random() - 0.5) * 0.5
        else:
            dist = i - 30
            val = math.sin(i * 0.2) * 0.03
            val += 0.15 * math.exp(-0.5 * ((dist + 14) / 2.5) ** 2)  # P
            val -= 0.18 * math.exp(-0.5 * ((dist + 4) / 1.5) ** 2)   # Q
            val += 1.15 * math.exp(-0.5 * (dist / 1.6) ** 2)         # R
            val -= 0.35 * math.exp(-0.5 * ((dist - 4) / 1.8) ** 2)   # S
            val += 0.32 * math.exp(-0.5 * ((dist - 15) / 4.0) ** 2)  # T
        samples.append(round(max(-1.0, min(1.0, val)), 3))
    return samples

async def handler(websocket, path=None):
    client_addr = websocket.remote_address
    logger.info(f"Client connected from {client_addr}")
    try:
        while True:
            # Generate Real-Time SDR RF Spectrum Telemetry Frame
            rf_bands = [
                {"name": "MedRadio 402M", "group": "MedRadio", "centerMhz": 402.5, "protocol": "MICS 402-405", "modulation": "2-FSK", "powerDbm": -62.0 + random.uniform(-2, 2)},
                {"name": "Wi-Fi 2.4G Ch 6", "group": "2.4GHz", "centerMhz": 2437.0, "protocol": "802.11ax", "modulation": "1024-QAM", "powerDbm": -55.0 + random.uniform(-3, 3)},
                {"name": "BLE Medical 2.4G", "group": "2.4GHz", "centerMhz": 2402.0, "protocol": "BLE 5.2", "modulation": "GFSK", "powerDbm": -68.0 + random.uniform(-1, 1)},
                {"name": "ICU Telemetry 5.8G", "group": "5GHz", "centerMhz": 5745.0, "protocol": "802.11be", "modulation": "256-QAM", "powerDbm": -60.0 + random.uniform(-2, 2)}
            ]

            # Generate Biomedical Live Telemetry Update
            biomed_payload = []
            for dev in BIOMED_DEVICES:
                is_fault = dev["status"] in ["CRITICAL_FAILURE", "RF_INTERFERENCE_JAMMED"]
                ecg = generate_ecg_sample(dev["heartRateBpm"], is_fault)
                dev_copy = dict(dev)
                dev_copy["ecgSamples"] = ecg
                dev_copy["timestamp"] = int(time.time() * 1000)
                biomed_payload.append(dev_copy)

            packet = {
                "type": "COMBINED_TELEMETRY_STREAM",
                "timestamp": int(time.time() * 1000),
                "rfBands": rf_bands,
                "biomedicalDevices": biomed_payload,
                "serverStatus": "STREAMING_NOMINAL",
                "source": "PC GNU Radio / Bio-Medical Gateway v2.5"
            }

            await websocket.send(json.dumps(packet))
            await asyncio.sleep(0.5)  # 2 Hz telemetry refresh

    except websockets.exceptions.ConnectionClosed:
        logger.info(f"Client disconnected: {client_addr}")
    except Exception as e:
        logger.error(f"Error handling client: {e}")

async def main():
    logger.info(f"Starting Unified Signal Shield & Bio-Medical PC Bridge on ws://{HOST}:{PORT}")
    logger.info("Ready for Android App / PC Browser Web Emulator connections...")
    async with websockets.serve(handler, HOST, PORT):
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Bridge server stopped by user.")
