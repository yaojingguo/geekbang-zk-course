// Adapted from https://github.com/etcd-io/etcd/blob/master/clientv3/concurrency/mutex.go
// Copyright 2016 The etcd Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package etcd_code

import (
	"context"
	"fmt"
	v3 "go.etcd.io/etcd/clientv3"
	"go.etcd.io/etcd/clientv3/concurrency"
	pb "go.etcd.io/etcd/etcdserver/etcdserverpb"
)

type Mutex struct {
	s *concurrency.Session

	pfx   string
	myKey string
	myRev int64
	hdr   *pb.ResponseHeader
}

func NewMutex(s *concurrency.Session, pfx string) *Mutex {
	m := &Mutex{s, pfx + "/", "", -1, nil}
	m.init()
	return m
}

func (m *Mutex) init() {
	m.myKey = "\x00"
	m.myRev = -1
}

func (m *Mutex) Lock(ctx context.Context) error {
	s := m.s
	client := m.s.Client()

	m.myKey = fmt.Sprintf("%s%x", m.pfx, s.Lease())
	cmp := v3.Compare(v3.CreateRevision(m.myKey), "=", 0)
	put := v3.OpPut(m.myKey, "", v3.WithLease(s.Lease()))
	get := v3.OpGet(m.myKey)
	getOwner := v3.OpGet(m.pfx, v3.WithFirstCreate()...)
	resp, err := client.Txn(ctx).If(cmp).Then(put, getOwner).Else(get, getOwner).Commit()
	if err != nil {
		m.hdr = resp.Header
		return err
	}
	m.myRev = resp.Header.Revision
	if !resp.Succeeded {
		m.myRev = resp.Responses[0].GetResponseRange().Kvs[0].CreateRevision
	}

	// if no key on prefix / the minimum rev is key, already hold the lock
	ownerKey := resp.Responses[1].GetResponseRange().Kvs
	if len(ownerKey) == 0 || ownerKey[0].CreateRevision == m.myRev {
		return nil
	}

	// wait for deletion revisions prior to myKey
	hdr, werr := waitDeletes(ctx, client, m.pfx, m.myRev-1)
	// release lock key if wait failed
	if werr != nil {
		m.Unlock(client.Ctx())
	} else {
		m.hdr = hdr
	}
	return werr
}

func (m *Mutex) Unlock(ctx context.Context) error {
	client := m.s.Client()
	if _, err := client.Delete(ctx, m.myKey); err != nil {
		return err
	}
	m.init()
	return nil
}

