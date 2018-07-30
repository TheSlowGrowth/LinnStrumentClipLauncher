package com.theslowgrowth;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LinnstrumentClipLauncherExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("d241e7cf-ae45-493f-8fe8-4a3b0ef3d48d");
   
   public LinnstrumentClipLauncherExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "LinnStrument ClipLauncher";
   }
   
   @Override
   public String getAuthor()
   {
      return "TheSlowGrowth";
   }

   @Override
   public String getVersion()
   {
      return "0.1";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }
   
   @Override
   public String getHardwareVendor()
   {
      return "Roger Linn Design";
   }
   
   @Override
   public String getHardwareModel()
   {
      return "LinnStrument";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 6;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[]{"LinnStrument MIDI"}, new String[]{"LinnStrument MIDI"});
      }
      else if (platformType == PlatformType.MAC)
      {
         list.add(new String[]{"LinnStrument MIDI"}, new String[]{"LinnStrument MIDI"});
      }
      else if (platformType == PlatformType.LINUX)
      {
         list.add(new String[]{"LinnStrument MIDI"}, new String[]{"LinnStrument MIDI"});
      }
   }

   @Override
   public LinnstrumentClipLauncherExtension createInstance(final ControllerHost host)
   {
      return new LinnstrumentClipLauncherExtension(this, host);
   }
}
