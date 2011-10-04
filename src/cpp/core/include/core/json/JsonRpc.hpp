/*
 * JsonRpc.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_JSON_JSON_RPC_HPP
#define CORE_JSON_JSON_RPC_HPP

#include <string>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/optional.hpp>
#include <boost/unordered_map.hpp>

#include <core/Error.hpp>
#include <core/json/Json.hpp>

namespace core {
namespace http {
   class Response ;
}
}

namespace core {
namespace json {

// constants
extern const char * const kRpcResult;
extern const char * const kRpcError;
extern const char * const kJsonContentType ;
   

// jsonRpcCategory
const boost::system::error_category& jsonRpcCategory() ;


//
// json error codes
//
namespace errc {
enum errc_t {
   Success = 0,           // request succeeded
   
   // 
   //	Invocation Errors -- All of these errors are guaranteed to have occurred 
   //	prior to the execution of the method on the service
   //
   ConnectionError = 1,   // unable to connect to the service
   Unavailable = 2,       // service is currently unavailable
   Unauthorized = 3,      // client does not have required credentials
   InvalidClientId = 4,   // provided client id is invalid
   ParseError = 5,        // invalid json or an unexpected error during parsing
   InvalidRequest = 6,    // invalid json-rpc request
   MethodNotFound = 7,    // specified method not found on the server
   ParamMissing = 8,      // parameter missing
   ParamTypeMismatch = 9, // parameter type mismatch
   ParamInvalid = 10,     // parameter invalid
   MethodUnexpected = 11, // method unexpected for current application state
   InvalidClientVersion = 12, // client is running an invalid version
   ServerOffline = 13,    // server is offline
  
   // Execution errors -- These errors occurred during execution of the method.
   // Application state is therefore known based on the expected behavior
   // of the error which occurred. More details are provided within the
   // optional "error" field of the result
   ExecutionError = 100,

   // Transmission errors -- These errors leave the application in an unknown
   // state (it is not known whether the method finished all, some, or none
   // of its work).
   TransmissionError = 200
};

inline boost::system::error_code make_error_code( errc_t e )
{
   return boost::system::error_code( e, jsonRpcCategory() ); }

inline boost::system::error_condition make_error_condition( errc_t e )
{
   return boost::system::error_condition( e, jsonRpcCategory() );
}

} // namespace errc
} // namespace json
} // namespace core

namespace boost {
namespace system {
template <>
struct is_error_code_enum<core::json::errc::errc_t>
 { static const bool value = true; };
} // namespace system
} // namespace boost


namespace core {
namespace json {

struct JsonRpcRequest
{
   JsonRpcRequest() : version(0), isBackgroundConnection(false) {}
   
   std::string method ;
   json::Array params ;
   json::Object kwparams ;
   std::string clientId ;
   double version;
   bool isBackgroundConnection ;

   bool empty() const { return method.empty(); }
   
   void clear() 
   {
      method.clear() ;
      params.clear() ;
      kwparams.clear() ;
   }
};

// 
// json request parsing
//
Error parseJsonRpcRequest(const std::string& input, JsonRpcRequest* pRequest) ;

bool parseJsonRpcRequestForMethod(const std::string& input, 
                                  const std::string& method,
                                  JsonRpcRequest* pRequest,
                                  http::Response* pResponse);
   
   
//
// json parameter reading helpers
//
   

inline core::Error readParam(const json::Array& params, 
                             unsigned int index, 
                             json::Value* pValue)
{
   if (index >= params.size())
      return core::Error(errc::ParamMissing, ERROR_LOCATION);
   
   *pValue = params[index] ;
   return Success();
}

template <typename T>
core::Error readParam(const json::Array& params, unsigned int index, T* pValue)
{
   if (index >= params.size())
      return core::Error(errc::ParamMissing, ERROR_LOCATION);

   if (!isType<T>(params[index]))
      return core::Error(errc::ParamTypeMismatch, ERROR_LOCATION) ;

   *pValue = params[index].get_value<T>();

   return Success() ;
}
   
template <typename T1>
core::Error readParams(const json::Array& params, T1* pValue1)
{
   return readParam(params, 0, pValue1) ;
}

template <typename T1, typename T2>
core::Error readParams(const json::Array& params, T1* pValue1, T2* pValue2)
{
   core::Error error = readParam(params, 0, pValue1) ;
   if (error)
      return error ;

   return readParam(params, 1, pValue2) ;
}

template <typename T1, typename T2, typename T3>
core::Error readParams(const json::Array& params, 
                        T1* pValue1, 
                        T2* pValue2, 
                        T3* pValue3)
{
   core::Error error = readParams(params, pValue1, pValue2) ;
   if (error)
      return error ;

   return readParam(params, 2, pValue3) ;
}

template <typename T1, typename T2, typename T3, typename T4>
core::Error readParams(const json::Array& params, 
                       T1* pValue1, 
                       T2* pValue2, 
                       T3* pValue3,
                       T4* pValue4)
{
   core::Error error = readParams(params, pValue1, pValue2, pValue3) ;
   if (error)
      return error ;
   
   return readParam(params, 3, pValue4) ;
}

   
template <typename T1, typename T2, typename T3, typename T4, typename T5>
core::Error readParams(const json::Array& params, 
                       T1* pValue1, 
                       T2* pValue2, 
                       T3* pValue3,
                       T4* pValue4,
                       T5* pValue5)
{
   core::Error error = readParams(params, pValue1, pValue2, pValue3, pValue4) ;
   if (error)
      return error ;
   
   return readParam(params, 4, pValue5) ;
}

template <typename T1, typename T2, typename T3, typename T4, typename T5,
          typename T6>
core::Error readParams(const json::Array& params,
                       T1* pValue1,
                       T2* pValue2,
                       T3* pValue3,
                       T4* pValue4,
                       T5* pValue5,
                       T6* pValue6)
{
   core::Error error = readParams(params,
                                  pValue1,
                                  pValue2,
                                  pValue3, 
                                  pValue4,
                                  pValue5) ;
   if (error)
      return error ;
   
   return readParam(params, 5, pValue6) ;
}   
   
template <typename T1, typename T2, typename T3, typename T4, typename T5,
typename T6, typename T7>
core::Error readParams(const json::Array& params,
                       T1* pValue1,
                       T2* pValue2,
                       T3* pValue3,
                       T4* pValue4,
                       T5* pValue5,
                       T6* pValue6,
                       T7* pValue7)
{
   core::Error error = readParams(params,
                                  pValue1,
                                  pValue2,
                                  pValue3,
                                  pValue4,
                                  pValue5,
                                  pValue6) ;
   if (error)
      return error ;

   return readParam(params, 6, pValue7) ;
}

template <typename T1, typename T2, typename T3, typename T4, typename T5,
typename T6, typename T7, typename T8>
core::Error readParams(const json::Array& params,
                       T1* pValue1,
                       T2* pValue2,
                       T3* pValue3,
                       T4* pValue4,
                       T5* pValue5,
                       T6* pValue6,
                       T7* pValue7,
                       T8* pValue8)
{
   core::Error error = readParams(params,
                                  pValue1,
                                  pValue2,
                                  pValue3,
                                  pValue4,
                                  pValue5,
                                  pValue6,
                                  pValue7) ;
   if (error)
      return error ;

   return readParam(params, 7, pValue8) ;
}

template <typename T>
core::Error readObject(const json::Object& object, 
                       const std::string& name, 
                       T* pValue)
{
   json::Object::const_iterator it = object.find(name) ;
   if (it == object.end())
      return Error(errc::ParamMissing, ERROR_LOCATION) ;

   if (!isType<T>(it->second))
      return Error(errc::ParamTypeMismatch, ERROR_LOCATION) ;

   *pValue = it->second.get_value<T>() ;

   return Success() ;
}

template <typename T>
core::Error readObject(const json::Object& object,
                       const std::string& name,
                       const T& defaultValue,
                       T* pValue)
{
   json::Object::const_iterator it = object.find(name) ;
   if (it == object.end())
   {
      *pValue = defaultValue;
      return Success();
   }

   if (!isType<T>(it->second))
      return Error(errc::ParamTypeMismatch, ERROR_LOCATION) ;

   *pValue = it->second.get_value<T>() ;

   return Success() ;
}

template <typename T>
core::Error readObjectParam(const json::Array& params,
                            unsigned int index, 
                            const std::string& name, 
                            T* pValue)
{
   json::Object object;
   Error error = json::readParam(params, index, &object);
   if (error)
      return error;
   
   return readObject(object, name, pValue);
}
   

template <typename T1, typename T2>
core::Error readObject(const json::Object& object, 
                       const std::string& name1, T1* pValue1,
                       const std::string& name2, T2* pValue2)
{
   Error error = readObject(object, name1, pValue1);
   if (error)
      return error;
   
   return readObject(object, name2, pValue2);
}
   
template <typename T1, typename T2>
core::Error readObjectParam(const json::Array& params, 
                            unsigned int index,
                            const std::string& name1, T1* pValue1,
                            const std::string& name2, T2* pValue2)
{
   json::Object object;
   Error error = json::readParam(params, index, &object);
   if (error)
      return error;
   
   return readObject(object, name1, pValue1, name2, pValue2);
}

template <typename T1, typename T2, typename T3>
core::Error readObject(const json::Object& object, 
                       const std::string& name1, T1* pValue1,
                       const std::string& name2, T2* pValue2,
                       const std::string& name3, T3* pValue3)
{
   Error error = readObject(object, name1, pValue1, name2, pValue2);
   if (error)
      return error;
   
   return readObject(object, name3, pValue3);
}
   
template <typename T1, typename T2, typename T3>
core::Error readObjectParam(const json::Array& params,
                            unsigned int index,
                            const std::string& name1, T1* pValue1,
                            const std::string& name2, T2* pValue2,
                            const std::string& name3, T3* pValue3)
{
   json::Object object;
   Error error = json::readParam(params, index, &object);
   if (error)
      return error;

   return readObject(object, name1, pValue1, name2, pValue2, name3, pValue3);
}
   
template <typename T1, typename T2, typename T3, typename T4>
core::Error readObject(const json::Object& object, 
                       const std::string& name1, T1* pValue1,
                       const std::string& name2, T2* pValue2,
                       const std::string& name3, T3* pValue3,
                       const std::string& name4, T4* pValue4)
{
   Error error = readObject(object, 
                            name1, pValue1, 
                            name2, pValue2,
                            name3, pValue3);
   if (error)
      return error;
   
   return readObject(object, name4, pValue4);
}
   
template <typename T1, typename T2, typename T3, typename T4>
core::Error readObjectParam(const json::Array& params,
                            unsigned int index,
                            const std::string& name1, T1* pValue1,
                            const std::string& name2, T2* pValue2,
                            const std::string& name3, T3* pValue3,
                            const std::string& name4, T4* pValue4)
{
   json::Object object;
   Error error = json::readParam(params, index, &object);
   if (error)
      return error;
   
   return readObject(object, 
                     name1, pValue1, 
                     name2, pValue2, 
                     name3, pValue3,
                     name4, pValue4);
}


template <typename T1, typename T2, typename T3, typename T4, typename T5>
core::Error readObject(const json::Object& object,
                       const std::string& name1, T1* pValue1,
                       const std::string& name2, T2* pValue2,
                       const std::string& name3, T3* pValue3,
                       const std::string& name4, T4* pValue4,
                       const std::string& name5, T5* pValue5)
{
   Error error = readObject(object,
                            name1, pValue1,
                            name2, pValue2,
                            name3, pValue3,
                            name4, pValue4);
   if (error)
      return error;

   return readObject(object, name5, pValue5);
}

template <typename T1, typename T2, typename T3, typename T4, typename T5>
core::Error readObjectParam(const json::Array& params,
                            unsigned int index,
                            const std::string& name1, T1* pValue1,
                            const std::string& name2, T2* pValue2,
                            const std::string& name3, T3* pValue3,
                            const std::string& name4, T4* pValue4,
                            const std::string& name5, T5* pValue5)
{
   json::Object object;
   Error error = json::readParam(params, index, &object);
   if (error)
      return error;

   return readObject(object,
                     name1, pValue1,
                     name2, pValue2,
                     name3, pValue3,
                     name4, pValue4,
                     name5, pValue5);
}


template <typename T1, typename T2, typename T3, typename T4, typename T5, typename T6>
core::Error readObject(const json::Object& object,
                       const std::string& name1, T1* pValue1,
                       const std::string& name2, T2* pValue2,
                       const std::string& name3, T3* pValue3,
                       const std::string& name4, T4* pValue4,
                       const std::string& name5, T5* pValue5,
                       const std::string& name6, T6* pValue6)
{
   Error error = readObject(object,
                            name1, pValue1,
                            name2, pValue2,
                            name3, pValue3,
                            name4, pValue4,
                            name5, pValue5);
   if (error)
      return error;

   return readObject(object, name6, pValue6);
}


template <typename T1, typename T2, typename T3, typename T4, typename T5, typename T6>
core::Error readObjectParam(const json::Array& params,
                            unsigned int index,
                            const std::string& name1, T1* pValue1,
                            const std::string& name2, T2* pValue2,
                            const std::string& name3, T3* pValue3,
                            const std::string& name4, T4* pValue4,
                            const std::string& name5, T5* pValue5,
                            const std::string& name6, T6* pValue6)
{
   json::Object object;
   Error error = json::readParam(params, index, &object);
   if (error)
      return error;

   return readObject(object,
                     name1, pValue1,
                     name2, pValue2,
                     name3, pValue3,
                     name4, pValue4,
                     name5, pValue5,
                     name6, pValue6);
}


template <typename T1, typename T2, typename T3, typename T4, typename T5, typename T6, typename T7>
core::Error readObject(const json::Object& object,
                       const std::string& name1, T1* pValue1,
                       const std::string& name2, T2* pValue2,
                       const std::string& name3, T3* pValue3,
                       const std::string& name4, T4* pValue4,
                       const std::string& name5, T5* pValue5,
                       const std::string& name6, T6* pValue6,
                       const std::string& name7, T7* pValue7)
{
   Error error = readObject(object,
                            name1, pValue1,
                            name2, pValue2,
                            name3, pValue3,
                            name4, pValue4,
                            name5, pValue5,
                            name6, pValue6);
   if (error)
      return error;

   return readObject(object, name7, pValue7);
}

template <typename T1, typename T2, typename T3, typename T4, typename T5, typename T6, typename T7>
core::Error readObjectParam(const json::Array& params,
                            unsigned int index,
                            const std::string& name1, T1* pValue1,
                            const std::string& name2, T2* pValue2,
                            const std::string& name3, T3* pValue3,
                            const std::string& name4, T4* pValue4,
                            const std::string& name5, T5* pValue5,
                            const std::string& name6, T6* pValue6,
                            const std::string& name7, T7* pValue7)
{
   json::Object object;
   Error error = json::readParam(params, index, &object);
   if (error)
      return error;

   return readObject(object,
                     name1, pValue1,
                     name2, pValue2,
                     name3, pValue3,
                     name4, pValue4,
                     name5, pValue5,
                     name6, pValue6,
                     name7, pValue7);
}

template <typename T1, typename T2, typename T3, typename T4, typename T5, typename T6, typename T7, typename T8>
core::Error readObject(const json::Object& object,
                       const std::string& name1, T1* pValue1,
                       const std::string& name2, T2* pValue2,
                       const std::string& name3, T3* pValue3,
                       const std::string& name4, T4* pValue4,
                       const std::string& name5, T5* pValue5,
                       const std::string& name6, T6* pValue6,
                       const std::string& name7, T7* pValue7,
                       const std::string& name8, T8* pValue8)
{
   Error error = readObject(object,
                            name1, pValue1,
                            name2, pValue2,
                            name3, pValue3,
                            name4, pValue4,
                            name5, pValue5,
                            name6, pValue6,
                            name7, pValue7);
   if (error)
      return error;

   return readObject(object, name8, pValue8);
}

template <typename T1, typename T2, typename T3, typename T4, typename T5, typename T6, typename T7, typename T8>
core::Error readObjectParam(const json::Array& params,
                            unsigned int index,
                            const std::string& name1, T1* pValue1,
                            const std::string& name2, T2* pValue2,
                            const std::string& name3, T3* pValue3,
                            const std::string& name4, T4* pValue4,
                            const std::string& name5, T5* pValue5,
                            const std::string& name6, T6* pValue6,
                            const std::string& name7, T7* pValue7,
                            const std::string& name8, T8* pValue8)
{
   json::Object object;
   Error error = json::readParam(params, index, &object);
   if (error)
      return error;

   return readObject(object,
                     name1, pValue1,
                     name2, pValue2,
                     name3, pValue3,
                     name4, pValue4,
                     name5, pValue5,
                     name6, pValue6,
                     name7, pValue7,
                     name8, pValue8);
}


// json rpc response
         
class JsonRpcResponse
{
public:
   JsonRpcResponse() : suppressDetectChanges_(false)
   {
      setResult(json::Value());
   };

   // COPYING: via compiler (copyable members)
   
public:
   
   template <typename T>
   void setResult(const T& result)
   {
      setField(kRpcResult, result);
   }
   
   void setError(const core::Error& error);
   
   void setError(const boost::system::error_code& ec);

   void setAsyncHandle(const std::string& handle);

   void setField(const std::string& name, const json::Value& value) 
   { 
      response_[name] = value;
   }             
                
   template <typename T>
   void setField(const std::string& name, const T& value) 
   { 
      setField(name, json::Value(value));
   }
   
   // low level hook to set the full response
   void setResponse(const json::Object& response)
   {
      response_ = response;
   }
   
   // specify a function to run after the response
   void setAfterResponse(const boost::function<void()>& afterResponse);
   bool hasAfterResponse() const;
   void runAfterResponse();

   bool suppressDetectChanges() { return suppressDetectChanges_; }
   void setSuppressDetectChanges(bool suppress)
   {
      suppressDetectChanges_ = suppress;
   }

   json::Object getRawResponse();
   
   void write(std::ostream& os) const;
   
private:
   json::Object response_;
   boost::function<void()> afterResponse_ ;
   bool suppressDetectChanges_;
};
   
   
// convenience functions for sending json-rpc responses
   
void setJsonRpcResponse(const JsonRpcResponse& jsonRpcResponse,
                        http::Response* pResponse); 


inline void setVoidJsonRpcResult(http::Response* pResponse)
{
   JsonRpcResponse jsonRpcResponse;
   setJsonRpcResponse(jsonRpcResponse, pResponse);
}   


template <typename T>
void setJsonRpcResult(const T& result, http::Response* pResponse)
{
   JsonRpcResponse jsonRpcResponse ;
   jsonRpcResponse.setResult(result);
   setJsonRpcResponse(jsonRpcResponse, pResponse);
}   

template <typename T>
void setJsonRpcError(const T& error, core::http::Response* pResponse)
{   
   JsonRpcResponse jsonRpcResponse ;
   jsonRpcResponse.setError(error);
   setJsonRpcResponse(jsonRpcResponse, pResponse);
}


// convenience typedefs for managing a map of json rpc functions
typedef boost::function<core::Error(const core::json::JsonRpcRequest&, core::json::JsonRpcResponse*)>
      JsonRpcFunction ;
typedef std::pair<std::string,core::json::JsonRpcFunction>
      JsonRpcMethod ;
typedef boost::unordered_map<std::string,JsonRpcFunction>
      JsonRpcMethods;

/*
   Async method support -- JsonRpcAsyncFunction is intended for potentially
   long running operations that need to keep the HTTP connection open until
   their work is done. (See registerRpcAsyncCoupleMethod for a different
   mechanism that provides similar functionality, but closes the HTTP
   connection and uses an event to simulate returning a result to the client.)
*/

// JsonRpcFunctionContinuation is what a JsonRpcAsyncFunction needs to call
// when its work is complete
typedef boost::function<void(const core::Error&, core::json::JsonRpcResponse*)>
      JsonRpcFunctionContinuation ;
typedef boost::function<void(const core::json::JsonRpcRequest&, const JsonRpcFunctionContinuation&)>
      JsonRpcAsyncFunction ;
// The bool in the next two typedefs specifies whether the function wants the
// HTTP connection to stay open until the method finishes executing (direct return),
// or for the HTTP connection to immediate return with an "asyncHandle" value that
// can be used to look in the event stream later when the method completes (indirect
// return). Direct return provides lower latency for short operations, and indirect
// return must be used for longer-running operations to prevent the browser from
// being starved of available HTTP connections to the server.
typedef std::pair<std::string,std::pair<bool, core::json::JsonRpcAsyncFunction> >
      JsonRpcAsyncMethod ;
typedef boost::unordered_map<std::string,std::pair<bool, JsonRpcAsyncFunction> >
      JsonRpcAsyncMethods ;

JsonRpcAsyncFunction adaptToAsync(JsonRpcFunction synchronousFunction);
JsonRpcAsyncMethod adaptMethodToAsync(JsonRpcMethod synchronousMethod);

} // namespace json
} // namespace core



#endif // CORE_JSON_JSON_RPC_HPP

