package com.netflix.simianarmy.aws.janitor.rule.volume;

import java.util.Date;

import org.apache.commons.lang.Validate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.janitor.JanitorMonkey;

public abstract class VolumeAttachment {
	
	/** The Constant LOGGER. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(VolumeAttachment.class);
    
    /** The date format used to print or parse the user specified termination date. **/
	public static final DateTimeFormatter TERMINATION_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

  public Boolean volumeIsTagged(Resource resource) {
      
	  Validate.notNull(resource);
      if (!resource.getResourceType().name().equals("EBS_VOLUME")) {
          return true;
      }

      // The state of the volume being "available" means that it is not attached to any instance.
      if (!"available".equals(((AWSResource) resource).getAWSResourceState())) {
          return true;
      }
	  String janitorTag = resource.getTag(JanitorMonkey.JANITOR_TAG);
      if (janitorTag != null) {
          if ("donotmark".equals(janitorTag)) {
              LOGGER.info(String.format("The volume %s is tagged as not handled by Janitor",
                      resource.getId()));
              return true;
          }
          try {
              // Owners can tag the volume with a termination date in the "janitor" tag.
              Date userSpecifiedDate = new Date(
                      TERMINATION_DATE_FORMATTER.parseDateTime(janitorTag).getMillis());
              resource.setExpectedTerminationTime(userSpecifiedDate);
              resource.setTerminationReason(String.format("User specified termination date %s", janitorTag));
              return false;
          } catch (Exception e) {
              LOGGER.error(String.format("The janitor tag is not a user specified date: %s", janitorTag));
          }
      }
      return null;
   }

}
