<?php

error_reporting(E_ALL);
ini_set('display_errors',1);

include('dbcon_capstone.php');

// 업데이트
$id=isset($_POST['id']) ? $_POST['id'] : '';
$positive=isset($_POST['positive']) ? $_POST['positive'] : '';
$havetopay=isset($_POST['havetopay']) ? $_POST['havetopay'] : '';
$email=isset($_POST['email']) ? $_POST['email'] : '';
$android = strpos($_SERVER['HTTP_USER_AGENT'], "Android");

if($havetopay != '' && $email != '')   // 사용 종료
{
  $stmt = $con->prepare('UPDATE members SET havetopay = :havetopay WHERE email = :email;
  UPDATE target SET positive = :positive WHERE id = :id');  //준비
  $stmt->bindParam(':havetopay', $havetopay);
  $stmt->bindParam(':email', $email);
  $stmt->bindParam(':id', $id);
  $stmt->bindParam(':positive', $positive);
}
else    // 사용 시작
{
  $stmt = $con->prepare('UPDATE target SET positive = :positive WHERE id = :id');  //준비
  $stmt->bindParam(':id', $id);
  $stmt->bindParam(':positive', $positive);
}
  $stmt->execute();


// 다시 읽음
$stmt = $con->prepare('select * from target');
$stmt->execute();

if ($stmt->rowCount() > 0)
{
    $data = array();

    while($row=$stmt->fetch(PDO::FETCH_ASSOC))
    {
        extract($row);

        array_push($data,
            array('id'=>$id,
            'lati'=>$lati,
            'longi'=>$longi,
            'positive'=>$positive
        ));
    }

    header('Content-Type: application/json; charset=utf8');
    $json = json_encode(array("webnautes"=>$data), JSON_PRETTY_PRINT+JSON_UNESCAPED_UNICODE);
    echo $json;
}


?>
