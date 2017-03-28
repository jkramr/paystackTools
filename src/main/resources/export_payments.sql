SELECT
  r.id,
  r.type,
  r.token,
  r.token_data,
  p.created,
  r.amount,
  r.payment_id,
  p.user_id,
  p.payment_method_id,
  p.payment_method_type,
  r.state,
  s.generated_email,
  r.is_auto_retry,
  r.previous_row_id,
  u.value
FROM `payment` AS p
  JOIN `payment_row` AS r ON (r.payment_id = p.id)
  JOIN `user_metadata` AS u ON (p.user_id = u.user_id)
  JOIN `user` AS s ON (p.user_id = s.id)
WHERE p.payment_method_type = 'paystack'
ORDER BY p.created DESC;