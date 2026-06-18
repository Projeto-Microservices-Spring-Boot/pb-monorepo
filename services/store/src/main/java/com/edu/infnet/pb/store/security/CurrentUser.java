package com.edu.infnet.pb.store.security;

import java.lang.annotation.*;

/**
 * Anotação para injetar o usuário autenticado nos controllers.
 * Ex: public ResponseEntity<?> meuEndpoint(@CurrentUser UserPrincipal user) { ... }
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
