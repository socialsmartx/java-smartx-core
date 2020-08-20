package com.smartx.api.v1.model;

import com.smartx.api.v1.model.ApiHandlerResponse;
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

public class DoTransactionResponse extends ApiHandlerResponse {
  
  @ApiModelProperty(value = "The transaction hash")
 /**
   * The transaction hash  
  **/
  private String result = null;
 /**
   * The transaction hash
   * @return result
  **/
  @JsonProperty("result")
 @Pattern(regexp="^(0x)?[0-9a-fA-F]{64}$")  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public DoTransactionResponse result(String result) {
    this.result = result;
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DoTransactionResponse {\n");
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

