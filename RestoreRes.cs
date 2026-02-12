using System;
using System.Runtime.InteropServices;

public class RestoreRes {
    [DllImport("user32.dll", CharSet = CharSet.Ansi)]
    public static extern bool EnumDisplaySettings(string deviceName, int modeNum, ref DEVMODE devMode);
    
    [DllImport("user32.dll", CharSet = CharSet.Ansi)]
    public static extern int ChangeDisplaySettings(ref DEVMODE devMode, int flags);

    public const int ENUM_CURRENT_SETTINGS = -1;
    public const int ENUM_REGISTRY_SETTINGS = -2;
    public const int CDS_UPDATEREGISTRY = 0x01;
    public const int DISP_CHANGE_SUCCESSFUL = 0;

    [StructLayout(LayoutKind.Sequential)]
    public struct DEVMODE {
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 32)]
        public string dmDeviceName;
        public short dmSpecVersion;
        public short dmDriverVersion;
        public short dmSize;
        public short dmDriverExtra;
        public int dmFields;
        public int dmPositionX;
        public int dmPositionY;
        public int dmDisplayOrientation;
        public int dmDisplayFixedOutput;
        public short dmColor;
        public short dmDuplex;
        public short dmYResolution;
        public short dmTTOption;
        public short dmCollate;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 32)]
        public string dmFormName;
        public short dmLogPixels;
        public int dmBitsPerPel;
        public int dmPelsWidth;
        public int dmPelsHeight;
        public int dmDisplayFlags;
        public int dmDisplayFrequency;
        public int dmICMMethod;
        public int dmICMIntent;
        public int dmMediaType;
        public int dmDitherType;
        public int dmReserved1;
        public int dmReserved2;
        public int dmPanningWidth;
        public int dmPanningHeight;
    }

    public static void Main(string[] args) {
        DEVMODE devMode = new DEVMODE();
        devMode.dmSize = (short)Marshal.SizeOf(devMode);

        Console.WriteLine("DEVMODE size: " + devMode.dmSize);

        DEVMODE bestMode = default(DEVMODE);
        bool found = false;
        int i = 0;

        // Try to enumerate modes
        while (EnumDisplaySettings(null, i, ref devMode)) {
            // Console.WriteLine("Mode " + i + ": " + devMode.dmPelsWidth + "x" + devMode.dmPelsHeight);
            if (devMode.dmBitsPerPel == 32) {
                if (!found) {
                    bestMode = devMode;
                    found = true;
                } else {
                     long currentPixels = (long)devMode.dmPelsWidth * devMode.dmPelsHeight;
                     long bestPixels = (long)bestMode.dmPelsWidth * bestMode.dmPelsHeight;
                     
                     if (currentPixels > bestPixels) {
                         bestMode = devMode;
                     } else if (currentPixels == bestPixels) {
                         if (devMode.dmDisplayFrequency > bestMode.dmDisplayFrequency) {
                             bestMode = devMode;
                         }
                     }
                }
            }
            i++;
        }

        if (found) {
            Console.WriteLine("Found best mode: " + bestMode.dmPelsWidth + "x" + bestMode.dmPelsHeight + " @ " + bestMode.dmDisplayFrequency + "Hz");
            
            // Only apply if users wants (in this case, user asked for it)
            int result = ChangeDisplaySettings(ref bestMode, CDS_UPDATEREGISTRY);
            if (result == DISP_CHANGE_SUCCESSFUL) {
                Console.WriteLine("Resolution changed successfully.");
            } else {
                Console.WriteLine("Failed to change resolution. Error code: " + result);
            }
        } else {
            Console.WriteLine("No suitable display mode found. Last Error: " + Marshal.GetLastWin32Error());
        }
    }
}
