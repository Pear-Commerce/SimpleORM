package com.ericdmartell.maga.objects;

import com.ericdmartell.maga.annotations.MAGAORMField;
import com.ericdmartell.maga.annotations.MAGATimestampID;

import java.util.Date;

@MAGATimestampID
public class DataMigrationRecord extends MAGAObject {

    @MAGAORMField(isIndex = true)
    public String name;

    @MAGAORMField
    public String order;

    @MAGAORMField
    public Date start;

    @MAGAORMField
    public Date end;
}
