package etcd_code

import (
	"testing"
)

func TestQueueBasics(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()

	queue := NewQueue(cli, "/queue")
	firstKey := "first"
	secondKey := "second"
	thirdKey := "third"

	enqueue(t, queue, firstKey)
	enqueue(t, queue, secondKey)
	enqueue(t, queue, thirdKey)

	//time.Sleep(100 * time.Second)

	dequeue(t, queue, firstKey)
	dequeue(t, queue, secondKey )
	dequeue(t, queue, thirdKey )
}

func enqueue(t *testing.T, queue *Queue, val string) {
	if err := queue.Enqueue(val); err != nil {
		t.Fatal(err)
	}
}

func dequeue(t *testing.T, queue *Queue, expectedVal string) {
	val, err := queue.Dequeue()
	if err != nil {
		t.Fatal(err)
	}
	if val != expectedVal {
		t.Errorf("expected val %s, but got %s", expectedVal, val)
	}
}
