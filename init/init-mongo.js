db = db.getSiblingDB("secure_order_db");

db.createUser({
  user: "teste",
  pwd: "password",
  roles: [{ role: "readWrite", db: "secure_order_db" }]
});

print("✅ Usuário 'teste' criado com sucesso no banco 'secure_order_db'!");