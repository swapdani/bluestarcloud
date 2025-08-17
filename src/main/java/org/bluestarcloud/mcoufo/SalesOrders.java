package org.bluestarcloud.mcoufo;

class SalesOrders {
    private String salesOrder;
    private String soItemDate;
    private String materialNo;
    private String partDesc;
    private Double ufoQty;
    private String uom;
    private String division;
    private String plant;
    private String plantDesc;
    private Double ufoValue;
    private String soldToPartyName;
    private String salesEmployeeName;
    private String salesOffice;
    private String salesOfficeDesc;
    private String salesOrderType;
    private String mcoRemarks;
    private Double qtyToBeArrenged;

    SalesOrders(String salesOrder, String soItemDate, String materialNo, String partDesc, Double ufoQty, String uom, String division, String plant, String plantDesc, Double ufoValue, String soldToPartyName, String salesEmployeeName, String salesOffice, String salesOfficeDesc, String salesOrderType) {
        this.salesOrder = salesOrder;
        this.soItemDate = soItemDate;
        this.materialNo = materialNo;
        this.partDesc = partDesc;
        this.ufoQty = ufoQty;
        this.uom = uom;
        this.division = division;
        this.plant = plant;
        this.plantDesc = plantDesc;
        this.ufoValue = ufoValue;
        this.soldToPartyName = soldToPartyName;
        this.salesEmployeeName = salesEmployeeName;
        this.salesOffice = salesOffice;
        this.salesOfficeDesc = salesOfficeDesc;
        this.salesOrderType = salesOrderType;
    }

    String getSalesOrder() {
        return salesOrder;
    }

    String getSoItemDate() {
        return soItemDate;
    }

    String getMaterialNo() {
        return materialNo;
    }

    String getPartDesc() {
        return partDesc;
    }

    Double getUfoQty() {
        return ufoQty;
    }

    String getUom() {
        return uom;
    }

    String getDivision() {
        return division;
    }

    String getPlant() {
        return plant;
    }

    String getPlantDesc() {
        return plantDesc;
    }

    Double getUfoValue() {
        return ufoValue;
    }

    String getSoldToPartyName() {
        return soldToPartyName;
    }

    String getSalesEmployeeName() {
        return salesEmployeeName;
    }

    String getSalesOffice() {
        return salesOffice;
    }

    String getSalesOfficeDesc() {
        return salesOfficeDesc;
    }

    String getSalesOrderType() {
        return salesOrderType;
    }

    String getMcoRemarks() {
        return mcoRemarks;
    }

    void setMcoRemarks(String mcoRemarks) {
        this.mcoRemarks = mcoRemarks;
    }
}
