package com.smartx.api.v1.model;

import com.smartx.api.v1.model.ApiHandlerResponse;
import com.smartx.api.v1.model.InfoType;
import javax.validation.constraints.*;

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetInfoResponse extends ApiHandlerResponse {
  
  @ApiModelProperty(value = "")
  private InfoType result = null;
 /**
   * Get result
   * @return result
  **/
  @JsonProperty("result")
  public InfoType getResult() {
    return result;
  }

  public void setResult(InfoType result) {
    this.result = result;
  }

  public GetInfoResponse result(InfoType result) {
    this.result = result;
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetInfoResponse {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    result: ").append(toIndentedString(result)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private static String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

