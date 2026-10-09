package com.example.smartwater.util;

import com.example.smartwater.dto.*;
import com.example.smartwater.entity.*;

import java.util.List;

/** Entity -> DTO conversion. Call inside a transaction (lazy associations are read). */
public final class Mappers {
    private Mappers() {}

    public static UserDtos.UserResponse toUser(User u) {
        return new UserDtos.UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(),
                u.getApartment() == null ? null : u.getApartment().getId(),
                u.getHousehold() == null ? null : u.getHousehold().getId(),
                u.getHousehold() == null ? null : u.getHousehold().getFlatNumber(),
                u.isEnabled());
    }

    public static ApartmentDtos.ApartmentResponse toApartment(Apartment a) {
        return new ApartmentDtos.ApartmentResponse(a.getId(), a.getName(), a.getAddress(), a.getCity(),
                a.getHouseholds().size(), a.getCreatedAt());
    }

    public static HouseholdDtos.HouseholdResponse toHousehold(Household h) {
        WaterMeter m = h.getMeter();
        return new HouseholdDtos.HouseholdResponse(h.getId(), h.getApartment().getId(), h.getApartment().getName(),
                h.getFlatNumber(), h.getHouseholdSize(), h.getAreaSqft(), h.isActive(),
                m == null ? null : m.getId(), m == null ? null : m.getMeterNumber(),
                h.getResidents().stream().map(User::getName).toList());
    }

    public static MeterDtos.MeterResponse toMeter(WaterMeter m) {
        return new MeterDtos.MeterResponse(m.getId(), m.getMeterNumber(), m.getHousehold().getId(),
                m.getHousehold().getFlatNumber(), m.getInstallationDate(), m.getStatus(),
                m.getInitialReading(), m.getCurrentReading());
    }

    public static UsageDtos.UsageLogResponse toUsage(WaterUsageLog l) {
        return new UsageDtos.UsageLogResponse(l.getId(), l.getHousehold().getId(), l.getHousehold().getFlatNumber(),
                l.getMeter().getId(), l.getMeter().getMeterNumber(), l.getReadingDate(), l.getMeterReading(),
                l.getConsumption(), l.getSource());
    }

    public static TariffDtos.TariffResponse toTariff(TariffPlan p) {
        List<TariffDtos.TierResponse> tiers = p.getTiers().stream()
                .map(t -> new TariffDtos.TierResponse(t.getId(), t.getTierOrder(), t.getFromKl(), t.getToKl(), t.getRatePerKl()))
                .toList();
        return new TariffDtos.TariffResponse(p.getId(), p.getApartment().getId(), p.getName(),
                p.getEffectiveFrom(), p.isActive(), tiers);
    }

    public static PurchaseDtos.PurchaseResponse toPurchase(WaterPurchase p) {
        return new PurchaseDtos.PurchaseResponse(p.getId(), p.getApartment().getId(),
                p.getBillingCycle() == null ? null : p.getBillingCycle().getId(), p.getSupplier(), p.getSupplyType(),
                p.getPurchaseDate(), p.getVolumeKl(), p.getUnitCost(), p.getTotalCost());
    }

    public static BillingDtos.InvoiceResponse toInvoice(Invoice i, String paymentInstructions) {
        BillingCycle c = i.getBillingCycle();
        List<BillingDtos.InvoiceItemResponse> items = i.getItems().stream()
                .map(it -> new BillingDtos.InvoiceItemResponse(it.getItemType(), it.getDescription(),
                        it.getQuantity(), it.getRate(), it.getAmount()))
                .toList();
        return new BillingDtos.InvoiceResponse(i.getId(), i.getInvoiceNumber(), i.getHousehold().getId(),
                i.getHousehold().getFlatNumber(), i.getResidentName(), c.getId(), c.getName(),
                c.getStartDate(), c.getEndDate(), i.getPreviousReading(), i.getCurrentReading(), i.getConsumption(),
                i.getWaterCharge(), i.getSharedCost(), i.getAdjustment(), i.getAdjustmentReason(),
                i.getTotalAmount(), i.getSharedCostBasis(), i.getPaymentStatus(), i.getCreatedAt(),
                items, paymentInstructions);
    }

    public static AlertDtos.AlertResponse toAlert(Alert a) {
        return new AlertDtos.AlertResponse(a.getId(), a.getHousehold().getId(), a.getHousehold().getFlatNumber(),
                a.getAlertType(), a.getSeverity(), a.getMessage(), a.getUsageDate(), a.isReadFlag(), a.getCreatedAt());
    }

    public static TipDtos.TipResponse toTip(WaterSavingTip t) {
        return new TipDtos.TipResponse(t.getId(), t.getTitle(), t.getContent());
    }
}
