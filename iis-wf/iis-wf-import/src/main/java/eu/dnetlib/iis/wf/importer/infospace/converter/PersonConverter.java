package eu.dnetlib.iis.wf.importer.infospace.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

import eu.dnetlib.data.proto.FieldTypeProtos.StringField;
import eu.dnetlib.data.proto.OafProtos.OafEntity;
import eu.dnetlib.iis.importer.schemas.Person;

/**
 * {@link OafEntity} containing person details to {@link Person} converter.
 * 
 * @author mhorst
 *
 */
public class PersonConverter implements OafEntityToAvroConverter<Person> {

    protected static final Logger log = Logger.getLogger(PersonConverter.class);

    // ------------------------ LOGIC --------------------------
    
    @Override
    public Person convert(OafEntity oafEntity) {
        Preconditions.checkNotNull(oafEntity);
        eu.dnetlib.data.proto.PersonProtos.Person sourcePerson = oafEntity.getPerson();
        if (sourcePerson != null && sourcePerson.getMetadata() != null) {
            Person.Builder builder = Person.newBuilder();
            builder.setId(oafEntity.getId());
            handleFirstName(sourcePerson.getMetadata().getFirstname() ,builder);
            handleSecondNames(sourcePerson.getMetadata().getSecondnamesList() ,builder);
            handleFullName(sourcePerson.getMetadata().getFullname() ,builder);
            return builder.build();
        } else {
            log.error("skipping: no metadata specified for person: " + oafEntity.getId());
            return null;
        }
    }
    
    // ------------------------ PRIVATE --------------------------
    
    private void handleFirstName(StringField firstName, Person.Builder builder) {
        if (firstName != null && !StringUtils.isEmpty(firstName.getValue())) {
            builder.setFirstname(firstName.getValue());
        }
    }
    
    private void handleSecondNames(List<StringField> secondNames, Person.Builder builder) {
        if (!CollectionUtils.isEmpty(secondNames)) {
            if (builder.getSecondnames() == null) {
                builder.setSecondnames(new ArrayList<CharSequence>(secondNames.size()));
            }
            List<CharSequence> resultNames = new ArrayList<CharSequence>(secondNames.size());
            for (StringField currentSecondName : secondNames) {
                if (currentSecondName.getValue() != null) {
                    resultNames.add(currentSecondName.getValue());
                }
            }
            builder.getSecondnames().addAll(resultNames);
        }
    }
    
    private void handleFullName(StringField fullName, Person.Builder builder) {
        if (fullName != null && !StringUtils.isEmpty(fullName.getValue())) {
            builder.setFullname(fullName.getValue());
        }
    }
    
}
