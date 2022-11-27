
package us.dot.its.jpo.geojsonconverter.pojos.spat;

import java.util.Objects;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.beanutils.BeanMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.ode.plugin.j2735.J2735BitString;
import us.dot.its.jpo.ode.plugin.j2735.J2735IntersectionStatusObject;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntersectionStatusObject {
    private static Logger logger = LoggerFactory.getLogger(IntersectionStatusObject.class);

    private boolean manualControlIsEnabled = false;
    private boolean stopTimeIsActivated = false;
    private boolean failureFlash = false;
    private boolean preemptIsActive = false;
    private boolean signalPriorityIsActive = false;
    private boolean fixedTimeOperation = false;
    private boolean trafficDependentOperation = false;
    private boolean standbyOperation = false;
    private boolean failureMode = false;
    private boolean off = false;
    private boolean recentMAPmessageUpdate = false;
    private boolean recentChangeInMAPassignedLanesIDsUsed = false;
    private boolean noValidMAPisAvailableAtThisTime = false;
    private boolean noValidSPATisAvailableAtThisTime = false;

    public boolean getManualControlIsEnabled() {
        return manualControlIsEnabled;
    }

    public void setManualControlIsEnabled(boolean manualControlIsEnabled) {
        this.manualControlIsEnabled = manualControlIsEnabled;
    }

    public boolean getStopTimeIsActivated() {
        return stopTimeIsActivated;
    }

    public void setStopTimeIsActivated(boolean stopTimeIsActivated) {
        this.stopTimeIsActivated = stopTimeIsActivated;
    }

    public boolean getFailureFlash() {
        return failureFlash;
    }

    public void setFailureFlash(boolean failureFlash) {
        this.failureFlash = failureFlash;
    }

    public boolean getPreemptIsActive() {
        return preemptIsActive;
    }

    public void setPreemptIsActive(boolean preemptIsActive) {
        this.preemptIsActive = preemptIsActive;
    }

    public boolean getSignalPriorityIsActive() {
        return signalPriorityIsActive;
    }

    public void setSignalPriorityIsActive(boolean signalPriorityIsActive) {
        this.signalPriorityIsActive = signalPriorityIsActive;
    }

    public boolean getFixedTimeOperation() {
        return fixedTimeOperation;
    }

    public void setFixedTimeOperation(boolean fixedTimeOperation) {
        this.fixedTimeOperation = fixedTimeOperation;
    }

    public boolean getTrafficDependentOperation() {
        return trafficDependentOperation;
    }

    public void setTrafficDependentOperation(boolean trafficDependentOperation) {
        this.trafficDependentOperation = trafficDependentOperation;
    }

    public boolean getStandbyOperation() {
        return standbyOperation;
    }

    public void setStandbyOperation(boolean standbyOperation) {
        this.standbyOperation = standbyOperation;
    }

    public boolean getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(boolean failureMode) {
        this.failureMode = failureMode;
    }

    public boolean getOff() {
        return off;
    }

    public void setOff(boolean off) {
        this.off = off;
    }

    public boolean getRecentMAPmessageUpdate() {
        return recentMAPmessageUpdate;
    }

    public void setRecentMAPmessageUpdate(boolean recentMAPmessageUpdate) {
        this.recentMAPmessageUpdate = recentMAPmessageUpdate;
    }

    public boolean getRecentChangeInMAPassignedLanesIDsUsed() {
        return recentChangeInMAPassignedLanesIDsUsed;
    }

    public void setRecentChangeInMAPassignedLanesIDsUsed(boolean recentChangeInMAPassignedLanesIDsUsed) {
        this.recentChangeInMAPassignedLanesIDsUsed = recentChangeInMAPassignedLanesIDsUsed;
    }

    public boolean getNoValidMAPisAvailableAtThisTime() {
        return noValidMAPisAvailableAtThisTime;
    }

    public void setNoValidMAPisAvailableAtThisTime(boolean noValidMAPisAvailableAtThisTime) {
        this.noValidMAPisAvailableAtThisTime = noValidMAPisAvailableAtThisTime;
    }

    public boolean getNoValidSPATisAvailableAtThisTime() {
        return noValidSPATisAvailableAtThisTime;
    }

    public void setNoValidSPATisAvailableAtThisTime(boolean noValidSPATisAvailableAtThisTime) {
        this.noValidSPATisAvailableAtThisTime = noValidSPATisAvailableAtThisTime;
    }

    public void setStatus(J2735BitString status) {
        var propertyMap = new BeanMap(this);
        if (status != null) {
            // Set each boolean property to the value from the status Bit String
            propertyMap.putAll(status);
        } else {
            // The Bit String is null, so set all the properties to false
            for (var enumValue : J2735IntersectionStatusObject.values()) {
                String propertyName = enumValue.toString();
                propertyMap.put(propertyName, false);
            }
        }

        
    }


    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IntersectionStatusObject)) {
            return false;
        }
        IntersectionStatusObject intersectionStatusObject = (IntersectionStatusObject) o;
        return manualControlIsEnabled == intersectionStatusObject.manualControlIsEnabled && stopTimeIsActivated == intersectionStatusObject.stopTimeIsActivated && failureFlash == intersectionStatusObject.failureFlash && preemptIsActive == intersectionStatusObject.preemptIsActive && signalPriorityIsActive == intersectionStatusObject.signalPriorityIsActive && fixedTimeOperation == intersectionStatusObject.fixedTimeOperation && trafficDependentOperation == intersectionStatusObject.trafficDependentOperation && standbyOperation == intersectionStatusObject.standbyOperation && failureMode == intersectionStatusObject.failureMode && off == intersectionStatusObject.off && recentMAPmessageUpdate == intersectionStatusObject.recentMAPmessageUpdate && recentChangeInMAPassignedLanesIDsUsed == intersectionStatusObject.recentChangeInMAPassignedLanesIDsUsed && noValidMAPisAvailableAtThisTime == intersectionStatusObject.noValidMAPisAvailableAtThisTime && noValidSPATisAvailableAtThisTime == intersectionStatusObject.noValidSPATisAvailableAtThisTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(manualControlIsEnabled, stopTimeIsActivated, failureFlash, preemptIsActive, signalPriorityIsActive, fixedTimeOperation, trafficDependentOperation, standbyOperation, failureMode, off, recentMAPmessageUpdate, recentChangeInMAPassignedLanesIDsUsed, noValidMAPisAvailableAtThisTime, noValidSPATisAvailableAtThisTime);
    }
    

    @Override
    public String toString() {
        ObjectMapper mapper = DateJsonMapper.getInstance();
        String testReturn = "";
        try {
            testReturn = (mapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        return testReturn;
    }

}
