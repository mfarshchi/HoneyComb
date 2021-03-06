package fi.vertx;

import fi.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by ashwin on 6/3/16.
 */
public final class RouterClass {
  /**
   * Constant for Success return.
   */
  private static final int SUCCESS = 200;
  /**
   * Constant ERROR for authentication return.
   */
  private static final int ERROR = 401;

  /**
   * The request could not be understood by the server due to malformed syntax.
   */
  private static final int BADREQUEST = 400;

  /**
   * Not acceptable, Returned by the Search API when an invalid format is
   * specified in the request.
   */
  private static final int NOTACCEPTABLE = 406;

  /**
   * The server is unavailable currently.
   */
  private static final int SERVICE_UNAVAILABLE = 503;

  /**
   * Not Found, The URI requested is invalid or the resource requested, such
   * as a user, does not exists.
   * Also returned when the requested format is not supported by the
   * requested method.
   */
  private static final int NOTFOUND = 404;

  /**
   * Test mysql.
   */
  private static final String testMysql = "MySQLTest.properties";

  /**
   * mysql
   */
  private static final String Mysql = "MySQL.properties";

  /**
   * Instantiates a new RouterClass. Private to prevent instantiation.
   */
  private RouterClass() {

    // Throw an exception if this ever *is* called
    throw new AssertionError("Instantiating utility class.");
  }

  /**
   * Login method responding to the post action.
   *
   * @param routingContext receives routing context from vertx.
   */
  static void login(final RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();

    User user = new User(request.getParam("username"),
        request.getParam("password"));
    boolean validUser = user.isValidUser(User.getFileName());
    HashMap<String, String> response = new HashMap<>();
    int responseCode;
    if (validUser) {
      response.put("Token", user.getApiToken());
      responseCode = SUCCESS;
    } else {
      response.put("error", "Invalid combination of username and "
          + "password.");
      responseCode = ERROR;
    }
    returnResponse(routingContext, responseCode, response);
    return;
  }

  /**
   * controller method to handle fault upload.
   *
   * @param routingContext routingcontext object
   */
  public static void uploadFault(RoutingContext routingContext) {
    Set<FileUpload> uploads = routingContext.fileUploads();
    FileUpload file = uploads.iterator().next();
    HttpServerRequest request = routingContext.request();
    HashMap<String, String> response = new HashMap<>();

    String token = request.getParam("token");
    File uploadedFault = new File(file.uploadedFileName());
    try {
      boolean validUser = User.isValidUser(token, User.getFileName());
      if (validUser) {
        String name = request.getParam("name");
        String desc = request.getParam("description");
        String args = request.getParam("arguments");
        if (file == null || name == null || name.equals("") || desc == null
            || desc.equals("")) {
          uploadedFault.delete();
          response.put("error", "Invalid request. Please check the " +
              "parameters");
          returnResponse(routingContext, BADREQUEST, response);
        } else {
          DbConnection dbCon = Utils.returnDbconnection(DbConnection
              .getFileName());
          if (FaultModel.getFaultByName(dbCon, name) == null) {
            FaultModel.insertFault(dbCon, name, desc, args);
            uploadedFault.renameTo(new File("faults/" + name + ".jar"));
            response.put("success", "Fault uploaded successfully");
            returnResponse(routingContext, SUCCESS, response);
          } else {
            uploadedFault.delete();
            response.put("error", "A fault with same name exists in the " +
                "system.");
            returnResponse(routingContext, BADREQUEST, response);
          }

        }

      } else {
        uploadedFault.delete();
        response.put("error", "You are not authorized to make this request.");
        returnResponse(routingContext, ERROR, response);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      uploadedFault.delete();
      response.put("error", "Something went wrong.Please try again later");
      returnResponse(routingContext, ERROR, response);
    }
  }

  /**
   * controller method to handle fault update.
   * has optional params of desc and arguments.
   *
   * @param routingContext routingcontext object
   */
  public static void updateFault(RoutingContext routingContext) {
    Set<FileUpload> uploads = routingContext.fileUploads();
    FileUpload file = uploads.iterator().next();
    HttpServerRequest request = routingContext.request();
    HashMap<String, String> response = new HashMap<>();
    String token = request.getParam("token");
    File uploadedFault = new File(file.uploadedFileName());
    try {
      boolean validUser = User.isValidUser(token, User.getFileName());
      if (validUser) {
        String faultId = request.getParam("faultId");
        String desc = request.getParam("description");
        String args = request.getParam("arguments");
        FaultModel fault = null;
        if (file == null || faultId == null || faultId.equals("")) {
          uploadedFault.delete();
          response.put("error", "Invalid request.Please check the parameters.");
          returnResponse(routingContext, BADREQUEST, response);
        } else {
          DbConnection dbCon = Utils.returnDbconnection(DbConnection
              .getFileName());
          fault = FaultModel.getFault(dbCon, faultId);
          if (fault != null) {
            if (fault.getActive()) {
              uploadedFault.delete();
              response.put("error", "Fault active. The existing fault needs " +
                  "to be disabled in order to be updated.");
              returnResponse(routingContext, BADREQUEST, response);
            } else {
              File oldFile = new File("faults/" + fault.getName() + ".jar");
              oldFile.delete();
              uploadedFault.renameTo(new File("faults/" + fault.getName() + "" +
                  ".jar"));
              FaultModel.updateFault(fault.getFaultId().toString(), dbCon,
                  true, desc, args);
              response.put("success", "Fault updated successfully");
              returnResponse(routingContext, SUCCESS, response);
            }

          } else {
            uploadedFault.delete();
            response.put("error", "A fault with given fault id  does not " +
                "exist in the system. Please use the upload feature.");
            returnResponse(routingContext, BADREQUEST, response);
          }

        }

      } else {
        uploadedFault.delete();
        response.put("error", "You are not authorized to make this request.");
        returnResponse(routingContext, ERROR, response);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      uploadedFault.delete();
      response.put("error", "Something went wrong.Please try again later");
      returnResponse(routingContext, ERROR, response);
    }
  }

  /**
   * Read the Logs, it's only used for the Demo website.
   *
   * @param routingContext receives routing context from vertx.
   */
  static void logs(final RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HashMap<String, String> response = new HashMap<>();
    String token = request.getParam("token");
    int responseCode;
    try {
      boolean validUser = User.isValidUser(token, User.getFileName());
      if (validUser) {
        File file = new File("src/main/resources/log");
        if (!file.exists())
          file.createNewFile();

        String thisLine = null;
        StringBuilder sb = new StringBuilder("");
        FileReader fileReader = new FileReader(file);
        BufferedReader br = new BufferedReader(fileReader);
        while ((thisLine = br.readLine()) != null) {
          sb.append(thisLine + "\n");
        }
        br.close();

        //System.out.println(sb.toString());

        response.put("logs", sb.toString());
        responseCode = SUCCESS;
        returnResponse(routingContext, responseCode, response);
        return;

      } else {
        response.put("error", "You are not authorized to make this request.");
        responseCode = ERROR;
        returnResponse(routingContext, responseCode, response);
        return;
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      response.put("error", "Something went wrong.Please try again later");
      responseCode = SERVICE_UNAVAILABLE;
      returnResponse(routingContext, responseCode, response);
      return;
    }
  }

  /**
   * FaultList method responding to the get action.
   *
   * @param routingContext receives routing context from vertx.
   */
  static void faultList(final RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HashMap<String, String> response = new HashMap<>();
    String token = request.getParam("token");
    int responseCode;
    try {
      boolean validUser = User.isValidUser(token, User.getFileName());
      if (validUser) {
        DbConnection dbCon = Utils.returnDbconnection(DbConnection
            .getFileName());
        List<FaultModel> list = FaultModel.getFaults(dbCon);
        responseCode = SUCCESS;
        dbCon.getConn().close();
        returnResponse(routingContext, responseCode, list);
        return;

      } else {
        response.put("error", "You are not authorized to make this request.");
        responseCode = ERROR;
        returnResponse(routingContext, responseCode, response);
        return;
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      response.put("error", "Something went wrong.Please try again later");
      responseCode = SERVICE_UNAVAILABLE;
      returnResponse(routingContext, responseCode, response);
      return;
    }
  }

  /**
   * Routing method for deactivating a fault.
   *
   * @param routingContext receives routing context from vertx.
   */
  static void deactivateFault(final RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HashMap<String, String> response = new HashMap<>();
    String faultId = request.getParam("faultId");
    String token = request.getParam("token");
    int responseCode;
    try {
      if (!Utils.isNumeric(faultId)) {
        response.put("error", "The parameter fault ID is not a number");
        responseCode = NOTACCEPTABLE;
        returnResponse(routingContext, responseCode, response);
        return;
      }
      boolean validUser = User.isValidUser(token, User.getFileName());
      if (validUser) {
        DbConnection dbCon = Utils.returnDbconnection(DbConnection
            .getFileName());
        Integer res = FaultModel.updateFault(faultId, dbCon, false, null, null);
        if (res == 0) {
          responseCode = NOTFOUND;
          response.put("error", "There is no fault in the system for the " +
              "given fault ID" +
              ".");
        } else {
          responseCode = SUCCESS;
          response.put("response", "Fault " + faultId + " has been " +
              "deactivated");
        }
        dbCon.getConn().close();
      } else {
        response.put("error", "You are not authorized to make this request.");
        responseCode = ERROR;
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      response.put("error", "Something went wrong.Please try again later");
      responseCode = SERVICE_UNAVAILABLE;
      returnResponse(routingContext, responseCode, response);
      return;
    }
    returnResponse(routingContext, responseCode, response);
    return;

  }

  /**
   * Routing method for reactivating a fault.
   *
   * @param routingContext receives routing context from vertx.
   */
  static void reactivateFault(final RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HashMap<String, String> response = new HashMap<>();
    String faultId = request.getParam("faultId");
    String token = request.getParam("token");
    int responseCode;
    try {
      if (!Utils.isNumeric(faultId)) {
        response.put("error", "The parameter fault ID is not a number");
        responseCode = NOTACCEPTABLE;
        returnResponse(routingContext, responseCode, response);
        return;
      }
      boolean validUser = User.isValidUser(token, User.getFileName());
      if (validUser) {
        DbConnection dbCon = Utils.returnDbconnection(DbConnection
            .getFileName());
        Integer res = FaultModel.updateFault(faultId, dbCon, true, null, null);
        if (res == 0) {
          responseCode = NOTFOUND;
          response.put("error", "There is no fault in the system for the " +
              "given fault ID.");
        } else {
          responseCode = SUCCESS;
          response.put("response", "Fault " + faultId + " has been " +
              "reactivated");
        }
        dbCon.getConn().close();
      } else {
        response.put("error", "You are not authorized to make this request.");
        responseCode = ERROR;
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      response.put("error", "Something went wrong.Please try again later");
      responseCode = SERVICE_UNAVAILABLE;
      returnResponse(routingContext, responseCode, response);
      return;
    }
    returnResponse(routingContext, responseCode, response);
    return;

  }

  /**
   * inject method responding to post action
   *
   * @param routingContext receives routing context from vertx.
   */
  static void inject(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HashMap<String, String> response = new HashMap<>();
    String token = request.getParam("token");
    String faultId = request.getParam("faultId");
    String faultInstanceId = null;
    int responseCode;
    try {
      if (!Utils.isNumeric(faultId)) {
        response.put("error", "The parameter fault ID is not a number.");
        responseCode = NOTACCEPTABLE;
        returnResponse(routingContext, responseCode, response);
        return;
      }
      boolean validUser = User.isValidUser(token, User.getFileName());
      if (validUser) {
        StringBuilder reason = new StringBuilder();

        DbConnection dbCon = Utils.returnDbconnection(Mysql);
        FaultModel fault = FaultModel.getFault(dbCon, faultId);

        if (fault == null) {
          response.put("error", "The requested fault doesn't exist.");
          responseCode = NOTFOUND;
          returnResponse(routingContext, responseCode, response);
          return;
        }

        FaultInjector injector = new FaultInjector(faultId, request.params());
        if (injector.validate(reason, Mysql)) {
          faultInstanceId = injector.inject(Mysql);
          response.put("success", "Fault injection initiated.");
          response.put("faultInstanceId", faultInstanceId);
          responseCode = SUCCESS;
          returnResponse(routingContext, responseCode, response);
          return;
        } else {
          if(reason.toString().equals("The requested fault is disabled."))
            response.put("error", reason.toString());
          else
            response.put("error", "missing argument: " + reason.toString());

          responseCode = NOTACCEPTABLE;
          returnResponse(routingContext, responseCode, response);
          return;
        }

      } else {
        response.put("error", "You are not authorized to make this request.");
        responseCode = ERROR;
        returnResponse(routingContext, responseCode, response);
        return;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      response.put("error", "Something went wrong.Please try again later");
      responseCode = SERVICE_UNAVAILABLE;
      returnResponse(routingContext, responseCode, response);
      return;
    }
  }

  /**
   * terminate a injection thread
   *
   * @param routingContext faultInstanceId
   */
  public static void termination(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HashMap<String, String> response = new HashMap<>();
    String token = request.getParam("token");
    String faultInstanceId = request.getParam("faultInstanceId");
    int responseCode;
    try {
      if (!Utils.isNumeric(faultInstanceId)) {
        response.put("error", "The parameter faultInstanceId is not a number");
        responseCode = NOTACCEPTABLE;
        returnResponse(routingContext, responseCode, response);
        return;
      }
      boolean validUser = User.isValidUser(token, User.getFileName());
      if (validUser) {
        if (FaultInjector.terminateFault(faultInstanceId) == 0) {
          response.put("success", "The requested fault instance has been " +
              "terminated.");
          response.put("faultInstanceId", faultInstanceId);
          responseCode = SUCCESS;
          returnResponse(routingContext, responseCode, response);
          return;
        } else {
          response.put("error", "Fault instance does not exist or has already" +
              " finished execution.");
          response.put("faultInstanceId", faultInstanceId);
          responseCode = NOTFOUND;
          returnResponse(routingContext, responseCode, response);
          return;
        }
      } else {
        response.put("error", "You are not authorized to make this request.");
        responseCode = ERROR;
        returnResponse(routingContext, responseCode, response);
        return;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      response.put("error", "Something went wrong.Please try again later");
      responseCode = SERVICE_UNAVAILABLE;
      returnResponse(routingContext, responseCode, response);
      return;
    }
  }

  /**
   * A Helper method to respond to request.
   *
   * @param routingContext Routing context object from vertx
   * @param responseCode   response code to send
   * @param response       response message to send
   */
  private static void returnResponse(final RoutingContext routingContext, int
      responseCode, Object response) {
    routingContext.response()
        .setStatusCode(responseCode)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(response));
  }
}
