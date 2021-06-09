package hello

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject

@VertxGen
@ProxyGen
interface MyProxyService {

  @Fluent
  fun get(query: JsonObject, handler: Handler<AsyncResult<JsonObject>>) : MyProxyService

}
