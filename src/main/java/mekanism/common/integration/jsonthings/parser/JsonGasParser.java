package mekanism.common.integration.jsonthings.parser;

import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import dev.gigaherz.jsonthings.util.parse.value.ObjValue;
import mekanism.api.MekanismAPI;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasBuilder;
import mekanism.api.chemical.gas.attribute.GasAttributes;
import mekanism.api.math.FloatingLong;
import mekanism.common.integration.LazyGasProvider;
import mekanism.common.integration.jsonthings.builder.JsonGasBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class JsonGasParser extends SimpleJsonChemicalParser<Gas, GasBuilder, JsonGasBuilder> {

    public JsonGasParser(IEventBus bus) {
        super(bus, ChemicalType.GAS, "Gas", MekanismAPI.gasRegistryName(), JsonGasBuilder::new);
    }

    @Override
    protected void processAttribute(JsonGasBuilder builder, ObjValue rawAttribute) {
        //Note: We chain ifKeys here as while there shouldn't be an overlap as it doesn't make sense, there is also nothing wrong
        // with allowing multiple attribute types to be defined in each block
        rawAttribute
              .ifKey("radioactivity", attribute -> attribute.doubleValue().handle(radioactivity -> builder.with(new GasAttributes.Radiation(radioactivity))))
              .ifKey("coolant", attribute -> {
                  ObjValue coolant = attribute.obj();
                  boolean hasCooledGas = coolant.hasKey("cooled_gas");
                  boolean hasHeatedGas = coolant.hasKey("heated_gas");
                  if (hasCooledGas == hasHeatedGas) {
                      //Error out if we are missing a cooled or heated gas or if both are declared
                      if (hasCooledGas) {
                          throw new ThingParseException("Coolants cannot declare both a cooled and heated gas");
                      }
                      throw new ThingParseException("Coolants must have either a 'cooled_gas' or a 'heated_gas'");
                  }
                  CoolantData coolantData = new CoolantData();
                  coolant.key("thermal_enthalpy", thermalEnthalpy -> thermalEnthalpy.doubleValue().handle(enthalpy -> coolantData.thermalEnthalpy = enthalpy))
                        .key("conductivity", conductivity -> conductivity.doubleValue().handle(c -> coolantData.conductivity = c));
                  coolant.key(hasCooledGas ? "cooled_gas" : "heated_gas", gas -> gas.string().map(ResourceLocation::new).handle(g -> coolantData.gas = g));
                  if (hasCooledGas) {
                      builder.with(new GasAttributes.HeatedCoolant(new LazyGasProvider(coolantData.gas), coolantData.thermalEnthalpy, coolantData.conductivity));
                  } else {
                      builder.with(new GasAttributes.CooledCoolant(new LazyGasProvider(coolantData.gas), coolantData.thermalEnthalpy, coolantData.conductivity));
                  }
              })
              .ifKey("fuel", attribute -> {
                  ObjValue fuel = attribute.obj();
                  FuelData fuelData = new FuelData();
                  fuel.key("burn_ticks", burnTicks -> burnTicks.intValue().handle(ticks -> fuelData.burnTicks = ticks));
                  fuel.key("energy_density", energyDensity -> energyDensity
                        .ifString(string -> string
                              .map(density -> FloatingLong.parseFloatingLong(density, true))
                              .handle(density -> fuelData.energyDensity = density)
                        )//TODO - 1.20: Figure out how we are handling this and the double one as both would get seen as the same?
                        /*.ifLong(l -> {
                            long density = l.getAsLong();
                            if (density < 0) {
                                throw new IllegalArgumentException("Energy cannot be negative!");
                            }
                            fuelData.energyDensity = FloatingLong.createConst(density);
                        }).ifDouble(d -> {
                            double density = d.getAsDouble();
                            if (density < 0) {
                                throw new IllegalArgumentException("Energy cannot be negative!");
                            }
                            fuelData.energyDensity = FloatingLong.createConst(density);
                        })*/
                  );
                  builder.with(new GasAttributes.Fuel(fuelData.burnTicks, fuelData.energyDensity));
              });
    }

    private static class CoolantData {

        @Nullable
        private ResourceLocation gas;
        private double thermalEnthalpy;
        private double conductivity;
    }

    private static class FuelData {

        private FloatingLong energyDensity = FloatingLong.ZERO;
        private int burnTicks;
    }
}